/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable2
import org.jetbrains.kotlin.backend.common.serialization.StringSignatureClashTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.util.StringSignature
import org.jetbrains.kotlin.ir.util.isPubliclyVisible
import org.jetbrains.kotlin.ir.util.render

class JsUniqIdClashTracker : StringSignatureClashTracker {
    private val committedIdSignatures = mutableMapOf<StringSignature, IrDeclaration>()

    override fun commit(declaration: IrDeclaration, signature: StringSignature) {
        if (!signature.isPubliclyVisible) return // don't track local ids

        if (signature in committedIdSignatures) {
            val clashedDeclaration = committedIdSignatures[signature]!!
            val parent = declaration.parent
            val clashedParent = clashedDeclaration.parent
            if (declaration !is IrTypeParameter || parent !is IrSimpleFunction || clashedParent !is IrSimpleFunction || parent.correspondingPropertySymbol !== clashedParent.correspondingPropertySymbol) {
                // TODO: handle clashes properly
                error("IdSignature clash: $signature; Existed declaration ${clashedDeclaration.render()} clashed with new ${declaration.render()}")
            }
        }

        committedIdSignatures[signature] = declaration
    }
}

class JsDeclarationTable(
    table: MutableMap<IrDeclaration, StringSignature>,
    builtIns: IrBuiltIns,
    tracker: StringSignatureClashTracker = JsUniqIdClashTracker()
) : DeclarationTable2(table, tracker) {
    init {
        loadKnownBuiltins(builtIns)
    }

}

//class JsGlobalDeclarationTable(builtIns: IrBuiltIns, tracker: StringSignatureClashTracker = JsUniqIdClashTracker()) :
//    GlobalDeclarationTable(JsManglerIr, tracker) {
//    init {
//        loadKnownBuiltins(builtIns)
//    }
//}