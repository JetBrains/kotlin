/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.Fir2IrSignatureComposer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.ir.util.IdSignature

class FirBasedSignatureComposer : Fir2IrSignatureComposer {

    class SignatureBuilder : FirVisitorVoid() {
        var hashId: Long? = null
        var mask = 0L

        private fun setExpected(f: Boolean) {
            mask = mask or IdSignature.Flags.IS_EXPECT.encode(f)
        }

        override fun visitElement(element: FirElement) {
            TODO("Not yet implemented")
        }

        override fun visitRegularClass(regularClass: FirRegularClass) {
            setExpected(regularClass.isExpect)
            //platformSpecificClass(descriptor)
        }
    }

    override fun composeSignature(declaration: FirDeclaration): IdSignature {
        val builder = SignatureBuilder()
        declaration.accept(builder)
        if (declaration is FirRegularClass && declaration.visibility != Visibilities.LOCAL) {
            val classId = declaration.classId
            return IdSignature.PublicSignature(classId.packageFqName, classId.relativeClassName, builder.hashId, builder.mask)
        }
        throw AssertionError("Unsupported FIR declaration in signature composer: ${declaration.render()}")
    }
}