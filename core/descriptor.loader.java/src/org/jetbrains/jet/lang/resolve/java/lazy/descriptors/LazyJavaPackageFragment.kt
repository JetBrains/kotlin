package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.utils.emptyList
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe

public class LazyJavaPackageFragment(
        c: LazyJavaResolverContext,
        containingDeclaration: NamespaceDescriptorParent,
        private val _fqName: FqName
) : AbstractNamespaceDescriptorImpl(
        containingDeclaration,
        emptyList(),
        if (_fqName.isRoot()) FqNameUnsafe.ROOT_NAME else _fqName.shortName()
    ), NamespaceDescriptor, LazyJavaDescriptor {

    private val _memberScope = LazyJavaPackageFragmentScope(c, this)

    override fun getMemberScope() = _memberScope
    override fun getFqName() = _fqName
}
