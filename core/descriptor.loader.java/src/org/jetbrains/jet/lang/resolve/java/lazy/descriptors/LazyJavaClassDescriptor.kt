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

class LazyJavaClassDescriptor(
        private val c: LazyJavaResolverContextWithTypes,
        containingDeclaration: DeclarationDescriptor,
        fqName: FqName,
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

    private val _scopeForMemberLookup = c.storageManager.createLazyValue {
        // TODO
        throw UnsupportedOperationException()
    }

    override fun getScopeForMemberLookup() = _scopeForMemberLookup()

    private val _thisAsReceiverParameter = c.storageManager.createLazyValue { DescriptorFactory.createLazyReceiverParameterDescriptor(this) }
    override fun getThisAsReceiverParameter() = _thisAsReceiverParameter()

    // TODO
    override fun getUnsubstitutedInnerClassesScope(): JetScope = JetScope.EMPTY

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    // TODO
    override fun getConstructors() = emptyOrSingletonList(getUnsubstitutedPrimaryConstructor())

    override fun getClassObjectType(): JetType? = null

    override fun getClassObjectDescriptor(): ClassDescriptor? = null

    // TODO
    override fun getAnnotations(): List<AnnotationDescriptor> = Collections.emptyList()

    private inner class LazyJavaClassTypeConstructor : TypeConstructor {

        private val _parameters = c.storageManager.createLazyValue {
            this@LazyJavaClassDescriptor.jClass.getTypeParameters().map {
                p ->
                innerC.typeParameterResolver.resolveTypeParameter(p)
                    ?: throw AssertionError("Parameter $p surely belongs to class $jClass, so it must be resolved")
            }
        }

        override fun getParameters(): List<TypeParameterDescriptor> = _parameters()

        private val _supertypes = c.storageManager.createLazyValue {
            jClass.getSupertypes().map {
                supertype ->
                innerC.typeResolver.transformJavaType(supertype, TypeUsage.SUPERTYPE)
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