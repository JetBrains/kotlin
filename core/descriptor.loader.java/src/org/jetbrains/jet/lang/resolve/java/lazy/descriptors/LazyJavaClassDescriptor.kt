package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.JavaClassResolver
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.DescriptorFactory
import org.jetbrains.jet.lang.resolve.java.lazy.child
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.resolve.java.lazy.resolveAnnotations
import org.jetbrains.jet.lang.resolve.java.lazy.types.toAttributes
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper
import org.jetbrains.jet.lang.resolve.java.resolver.JavaSupertypeResolver
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.utils.*
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.kotlin.util.iif
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaEnumClassObjectDescriptor
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler
import org.jetbrains.jet.lang.resolve.scopes.WritableScope

class LazyJavaClassDescriptor(
        private val outerC: LazyJavaResolverContextWithTypes,
        containingDeclaration: DeclarationDescriptor,
        internal val fqName: FqName,
        private val jClass: JavaClass
) : ClassDescriptorBase(outerC.storageManager, containingDeclaration, fqName.shortName()), LazyJavaDescriptor, JavaClassDescriptor {

    private val c: LazyJavaResolverContextWithTypes = outerC.child(this, jClass.getTypeParameters().toSet());

    {
        c.javaResolverCache.recordClass(jClass, this)
    }

    private val _kind = JavaClassResolver.determineClassKind(jClass)
    private val _modality = JavaClassResolver.determineClassModality(jClass)
    private val _visibility = jClass.getVisibility()
    private val _isInner = JavaClassResolver.isInnerClass(jClass)

    override fun getKind() = _kind
    override fun getModality() = _modality
    override fun getVisibility() = _visibility
    override fun isInner() = _isInner

    private val _typeConstructor = c.storageManager.createLazyValue { LazyJavaClassTypeConstructor() }
    override fun getTypeConstructor(): TypeConstructor = _typeConstructor()

    private val _scopeForMemberLookup = LazyJavaClassMemberScope(c, this, jClass)
    override fun getScopeForMemberLookup() = _scopeForMemberLookup

    private val _innerClassesScope = InnerClassesScopeWrapper(getScopeForMemberLookup())
    override fun getUnsubstitutedInnerClassesScope(): JetScope = _innerClassesScope

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    private val _classObjectDescriptor = c.storageManager.createNullableLazyValue {
        if (jClass.isEnum()) {
            val classObject = JavaEnumClassObjectDescriptor(this)
            classObject.setSupertypes(Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()))
            classObject.setModality(Modality.FINAL)
            classObject.setVisibility(jClass.getVisibility())
            classObject.setTypeParameterDescriptors(Collections.emptyList<TypeParameterDescriptor>())
            classObject.createTypeConstructor()

            val scope = LazyJavaClassMemberScope(c, classObject, jClass, enumClassObject = true)
            val writableScope = WritableScopeImpl(scope, classObject, RedeclarationHandler.THROW_EXCEPTION, "Enum class object scope")
            writableScope.changeLockLevel(WritableScope.LockLevel.BOTH)

            classObject.setScopeForMemberLookup(writableScope)

            JavaClassResolver.createEnumSyntheticMethods(classObject, this.getDefaultType())

            classObject
        }
        else null
    }
    override fun getClassObjectDescriptor(): ClassDescriptor? = _classObjectDescriptor()
    override fun getClassObjectType(): JetType? = getClassObjectDescriptor()?.let { d -> d.getDefaultType() }

    override fun getConstructors() = _scopeForMemberLookup._constructors()

    private val _annotations = c.storageManager.createLazyValue { c.resolveAnnotations(jClass.getAnnotations()) }
    override fun getAnnotations(): List<AnnotationDescriptor> = _annotations()

    private val _functionTypeForSamInterface = c.storageManager.createNullableLazyValue {
        val samInterfaceMethod = SingleAbstractMethodUtils.getSamInterfaceMethod(jClass);
        if (samInterfaceMethod != null) {
            val abstractMethod = resolveFunctionOfSamInterface(samInterfaceMethod);
            SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod);
        }
        else null
    }

    override fun getFunctionTypeForSamInterface(): JetType? = _functionTypeForSamInterface()

    private fun resolveFunctionOfSamInterface(samInterfaceMethod: JavaMethod): SimpleFunctionDescriptor {
        val methodContainer = samInterfaceMethod.getContainingClass()
        val containerFqName = methodContainer.getFqName()
        assert(containerFqName != null, "qualified name is null for " + methodContainer)
        if (fqName == containerFqName) {
            return _scopeForMemberLookup.resolveMethodToFunctionDescriptor(samInterfaceMethod, false)
        }
        else {
            return JavaClassResolver.findFunctionWithMostSpecificReturnType(TypeUtils.getAllSupertypes(getDefaultType()))
        }
    }

    override fun toString() = "lazy java class $fqName"

    private inner class LazyJavaClassTypeConstructor : TypeConstructor {

        private val _parameters = c.storageManager.createLazyValue {
            jClass.getTypeParameters().map({
                p ->
                c.typeParameterResolver.resolveTypeParameter(p)
                    ?: throw AssertionError("Parameter $p surely belongs to class ${jClass}, so it must be resolved")
            })
        }

        override fun getParameters(): List<TypeParameterDescriptor> = _parameters()

        private val _supertypes = c.storageManager.createLazyValue<Collection<JetType>> {
            val supertypes = jClass.getSupertypes()
            if (supertypes.isEmpty())
                if (jClass.getFqName() == JavaSupertypeResolver.OBJECT_FQ_NAME) {
                    listOf(KotlinBuiltIns.getInstance().getAnyType())
                }
                else {
                    val jlObject = c.javaClassResolver.resolveClassByFqName(JavaSupertypeResolver.OBJECT_FQ_NAME)?.getDefaultType()
                    // If java.lang.Object is not found, we simply use Any to recover
                    listOf(jlObject ?: KotlinBuiltIns.getInstance().getAnyType())
                }
            else
                supertypes.iterator()
                        .map {
                            supertype ->
                            c.typeResolver.transformJavaType(supertype, TypeUsage.SUPERTYPE.toAttributes())
                        }
                        .filter { supertype -> !supertype.isError() }
                        .toList()
                        .ifEmpty {
                            listOf(KotlinBuiltIns.getInstance().getAnyType())
                        }
        }

        override fun getSupertypes(): Collection<JetType> = _supertypes()

        override fun getAnnotations() = Collections.emptyList<AnnotationDescriptor>()

        override fun isFinal() = !getModality().isOverridable()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@LazyJavaClassDescriptor

        override fun toString(): String? = getName().asString()
    }
}