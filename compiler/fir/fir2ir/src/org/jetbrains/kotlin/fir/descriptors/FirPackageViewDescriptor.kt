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
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirPackageViewDescriptor(override val fqName: FqName, val moduleDescriptor: ModuleDescriptor) : PackageViewDescriptor {
    override fun getContainingDeclaration(): PackageViewDescriptor? {
        shouldNotBeCalled()
    }

    override val memberScope: MemberScope
        get() = MemberScope.Empty

    override val module: ModuleDescriptor
        get() = moduleDescriptor

    override val fragments: List<PackageFragmentDescriptor>
        get() = listOf(FirPackageFragmentDescriptor(fqName, moduleDescriptor))

    override fun getOriginal(): DeclarationDescriptor {
        shouldNotBeCalled()
    }

    override fun getName(): Name {
        shouldNotBeCalled()
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        shouldNotBeCalled()
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        shouldNotBeCalled()
    }

    override val annotations: Annotations
        get() = Annotations.EMPTY

}
