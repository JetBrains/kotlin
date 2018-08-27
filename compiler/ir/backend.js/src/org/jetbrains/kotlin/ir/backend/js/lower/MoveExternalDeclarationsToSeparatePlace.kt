/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.inline.addChild
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.name.FqName

class MoveExternalDeclarationsToSeparatePlace : FileLoweringPass {
    private val packageFragment = IrExternalPackageFragmentImpl(object : IrExternalPackageFragmentSymbol {
        override val descriptor: PackageFragmentDescriptor
            get() = error("Operation is unsupported")

        private var _owner: IrExternalPackageFragment? = null
        override val owner get() = _owner!!

        override val isBound get() = _owner != null

        override fun bind(owner: IrExternalPackageFragment) {
            _owner = owner
        }
    }, FqName.ROOT)

    override fun lower(irFile: IrFile) {
        val it = irFile.declarations.iterator()

        while (it.hasNext()) {
            val d = it.next()

            if (d.isEffectivelyExternal()) {
                it.remove()
                packageFragment.addChild(d)
            }
        }
    }
}