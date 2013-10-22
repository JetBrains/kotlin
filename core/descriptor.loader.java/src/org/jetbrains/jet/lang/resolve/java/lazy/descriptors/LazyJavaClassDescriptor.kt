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
import org.jetbrains.jet.utils.emptyOrSingletonList
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

class LazyJavaClassDescriptor(
        private val c: LazyJavaResolverContextWithTypes,
        containingDeclaration: DeclarationDescriptor,
        internal val fqName: FqName,
        private val jClass: JavaClass
) : ClassDescriptorBase(containingDeclaration, fqName.shortName()), LazyJavaDescriptor {

    private val innerC: LazyJavaResolverContextWithTypes = c.child(this, jClass.getTypeParameters().toSet())

    private val _kind = JavaClassResolver.determineClassKind(jClass)
    private val _modality = JavaClassResolver.determineClassModality(jClass)
    private val _visibility = jClass.getVisibility()
    private val _isInner = JavaClassResolver.isInnerClass(jClass)

    override fun getKind() = _kind
    override fun getModality() = _modality
    override fun getVisibility() = _visibility
    override fun isInner() = _isInner

    private val _typeConstructor = c.storageManager.createLazyValue { LazyJavaClassTypeConstructor() }
    override fun getTypeConstructor() = _typeConstructor()

    private val _scopeForMemberLookup = LazyJavaClassMemberScope(c, this, jClass, false)
    override fun getScopeForMemberLookup() = _scopeForMemberLookup

    private val _thisAsReceiverParameter = c.storageManager.createLazyValue { DescriptorFactory.createLazyReceiverParameterDescriptor(this) }
    override fun getThisAsReceiverParameter() = _thisAsReceiverParameter()

    private val _innerClassesScope = InnerClassesScopeWrapper(getScopeForMemberLookup())
    override fun getUnsubstitutedInnerClassesScope(): JetScope = _innerClassesScope

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    // TODO
    override fun getConstructors() = emptyOrSingletonList(getUnsubstitutedPrimaryConstructor())

    override fun getClassObjectType(): JetType? = null

    override fun getClassObjectDescriptor(): ClassDescriptor? = null

    private val _annotations = c.resolveAnnotations(jClass.getAnnotations())
    override fun getAnnotations(): List<AnnotationDescriptor> = _annotations

    private inner class LazyJavaClassTypeConstructor : TypeConstructor {

        private val _parameters = c.storageManager.createLazyValue {
            this@LazyJavaClassDescriptor.jClass.getTypeParameters().map {
                p ->
                innerC.typeParameterResolver.resolveTypeParameter(p)
                    ?: throw AssertionError("Parameter $p surely belongs to class $jClass, so it must be resolved")
            }
        }

        override fun getParameters(): List<TypeParameterDescriptor> = _parameters()

        private val _supertypes = c.storageManager.createLazyValue<Collection<JetType>> {
            val supertypes = jClass.getSupertypes()
            if (supertypes.isEmpty())
                if (jClass.getFqName() == JavaSupertypeResolver.OBJECT_FQ_NAME) {
                    listOf(KotlinBuiltIns.getInstance().getAnyType())
                }
                else {
                    val jlObject = innerC.javaClassResolver.resolveClassByFqName(JavaSupertypeResolver.OBJECT_FQ_NAME)?.getDefaultType()
                    // If java.lang.Object is not found, we simply use Any to recover
                    emptyOrSingletonList(jlObject ?: KotlinBuiltIns.getInstance().getAnyType())
                }
            else
                supertypes.map {
                    supertype ->
                    innerC.typeResolver.transformJavaType(supertype, TypeUsage.SUPERTYPE.toAttributes())
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