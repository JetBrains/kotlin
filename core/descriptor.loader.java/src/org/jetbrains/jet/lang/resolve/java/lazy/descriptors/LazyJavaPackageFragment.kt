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
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassStaticsPackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptorImpl

class LazyPackageFragmentForJavaPackage(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        jPackage: JavaPackage
) : LazyJavaPackageFragment(c, containingDeclaration, jPackage.getFqName(),
                            { LazyPackageFragmentScopeForJavaPackage(c, jPackage, this) })

class LazyPackageFragmentForJavaClass(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        private val jClass: JavaClass
) : JavaClassStaticsPackageFragmentDescriptor,
    LazyJavaPackageFragment(c, containingDeclaration,
                            jClass.getFqName().sure("Attempt to build a package of an anonymous/local class: $jClass"),
                            { LazyPackageFragmentScopeForJavaClass(c, jClass, this) }) {
    override fun getCorrespondingClass(): JavaClassDescriptor {
        val classDescriptor = c.javaClassResolver.resolveClass(jClass)
        if (classDescriptor !is JavaClassDescriptor) {
            throw AssertionError("Corresponding class not found for package with static members: $this")
        }
        return classDescriptor
    }
}

abstract class LazyJavaPackageFragment(
        protected val c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        fqName: FqName,
        createMemberScope: LazyJavaPackageFragment.() -> LazyJavaPackageFragmentScope
) : PackageFragmentDescriptorImpl(containingDeclaration, fqName),
    JavaPackageFragmentDescriptor, LazyJavaDescriptor
{
    private val _memberScope = createMemberScope()
    override fun getMemberScope(): LazyJavaPackageFragmentScope = _memberScope

    override fun toString() = "lazy java package fragment: " + fqName
}