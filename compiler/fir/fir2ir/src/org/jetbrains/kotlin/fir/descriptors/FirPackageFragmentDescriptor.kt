/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class FirPackageFragmentDescriptor(override val fqName: FqName, val moduleDescriptor: ModuleDescriptor) : PackageFragmentDescriptor {
    override fun getContainingDeclaration(): ModuleDescriptor {
        return moduleDescriptor
    }


    override fun getMemberScope(): MemberScope {
        TODO("not implemented")
    }

    override fun getOriginal(): DeclarationDescriptorWithSource {
        return this
    }

    override fun getName(): Name {
        return fqName.shortName()
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        TODO("not implemented")
    }

    override fun getSource(): SourceElement {
        TODO("not implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        TODO("not implemented")
    }

    override val annotations: Annotations
        get() = TODO("not implemented")

}