/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

open class FirPackageFragmentDescriptor(override val fqName: FqName, val moduleDescriptor: ModuleDescriptor) : PackageFragmentDescriptor {
    override fun getContainingDeclaration(): ModuleDescriptor {
        return moduleDescriptor
    }


    override fun getMemberScope(): MemberScope {
        return MemberScope.Empty
    }

    override fun getOriginal(): DeclarationDescriptorWithSource {
        return this
    }

    override fun getName(): Name {
        return fqName.shortName()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        return visitor?.visitPackageFragmentDescriptor(this, data) as R
    }

    override fun getSource(): SourceElement {
        TODO("not implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        visitor?.visitPackageFragmentDescriptor(this, null)
    }

    override val annotations: Annotations
        get() = Annotations.EMPTY

}