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

abstract class LazyJavaPackageFragment(
        private val c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        name: Name
) : DeclarationDescriptorNonRootImpl(containingDeclaration, listOf(), name), JavaPackageFragmentDescriptor, LazyJavaDescriptor {

    protected abstract val _memberScope: JetScope

    override fun getMemberScope() = _memberScope

    override fun getJavaDescriptorResolver(): JavaDescriptorResolver = c.javaDescriptorResolver

    override fun getProvider(): PackageFragmentProvider = c.packageFragmentProvider

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D) = visitor.visitPackageFragmentDescriptor(this, data) as R

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun toString() = "lazy java package fragment: " + getFqName()
}

public class LazyPackageFragmentForJavaPackage(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        val jPackage: JavaPackage
) : LazyJavaPackageFragment(c, containingDeclaration, jPackage.getFqName().shortNameOrSpecial()) {
    override fun getFqName(): FqName = jPackage.getFqName()

    override val _memberScope = LazyPackageFragmentScopeForJavaPackage(c, jPackage, this)

    override fun getKind() = JavaPackageFragmentDescriptor.Kind.PROPER
}

public class LazyPackageFragmentForJavaClass(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        val jClass: JavaClass
) : LazyJavaPackageFragment(c, containingDeclaration,
                            jClass.getFqName().sure("Attempt to build a package of an anonymous/local class: $jClass")
                                    .shortNameOrRoot()
) {
    override fun getFqName(): FqName = jClass.getFqName()!!

    override val _memberScope = LazyPackageFragmentScopeForJavaClass(c, jClass, this)

    override fun getKind() = JavaPackageFragmentDescriptor.Kind.CLASS_STATICS
}