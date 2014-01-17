package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorNonRootImpl
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.annotations.Annotations

fun LazyPackageFragmentForJavaPackage(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        jPackage: JavaPackage
) = LazyJavaPackageFragment(c, containingDeclaration, JavaPackageFragmentDescriptor.Kind.PROPER, jPackage.getFqName(),
                            { LazyPackageFragmentScopeForJavaPackage(c, jPackage, this) })

fun LazyPackageFragmentForJavaClass(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        jClass: JavaClass
) = LazyJavaPackageFragment(c, containingDeclaration, JavaPackageFragmentDescriptor.Kind.CLASS_STATICS,
                            jClass.getFqName().sure("Attempt to build a package of an anonymous/local class: $jClass"),
                            { LazyPackageFragmentScopeForJavaClass(c, jClass, this) })

class LazyJavaPackageFragment(
        private val c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        val _kind: JavaPackageFragmentDescriptor.Kind,
        val _fqName: FqName,
        createMemberScope: LazyJavaPackageFragment.() -> LazyJavaPackageFragmentScope
) : DeclarationDescriptorNonRootImpl(containingDeclaration, Annotations.EMPTY, _fqName.shortNameOrSpecial()),
    JavaPackageFragmentDescriptor, LazyJavaDescriptor
{
    private val _memberScope = createMemberScope()
    override fun getMemberScope(): LazyJavaPackageFragmentScope = _memberScope

    override fun getKind() = _kind
    override fun getFqName() = _fqName
    override fun getJavaDescriptorResolver() = c.javaDescriptorResolver
    override fun getProvider() = c.packageFragmentProvider

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D) = visitor.visitPackageFragmentDescriptor(this, data) as R

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun toString() = "lazy java package fragment: " + getFqName()
}