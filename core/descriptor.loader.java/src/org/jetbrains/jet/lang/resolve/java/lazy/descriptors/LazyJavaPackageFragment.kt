package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent
import org.jetbrains.jet.utils.emptyList
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.sure

abstract class LazyJavaPackageFragment(
        c: LazyJavaResolverContext,
        containingDeclaration: NamespaceDescriptorParent,
        name: Name
) : AbstractNamespaceDescriptorImpl(containingDeclaration, emptyList(), name), NamespaceDescriptor, LazyJavaDescriptor {

    protected abstract val _memberScope: JetScope

    override fun getMemberScope() = _memberScope
}

public class LazyPackageFragmentForJavaPackage(
        c: LazyJavaResolverContext,
        containingDeclaration: NamespaceDescriptorParent,
        val jPackage: JavaPackage
) : LazyJavaPackageFragment(c, containingDeclaration, jPackage.getFqName().shortNameOrSpecial()) {
    override fun getFqName(): FqName = jPackage.getFqName()

    override val _memberScope = LazyPackageFragmentScopeForJavaPackage(c, jPackage, this)
}

public class LazyPackageFragmentForJavaClass(
        c: LazyJavaResolverContext,
        containingDeclaration: NamespaceDescriptorParent,
        val jClass: JavaClass
) : LazyJavaPackageFragment(c, containingDeclaration,
                            jClass.getFqName().sure("Attempt to build a package of an anonymous/local class: $jClass")
                                    .shortNameOrRoot()
) {
    override fun getFqName(): FqName = jClass.getFqName()!!

    override val _memberScope = LazyPackageFragmentScopeForJavaClass(c, jClass, this)
}