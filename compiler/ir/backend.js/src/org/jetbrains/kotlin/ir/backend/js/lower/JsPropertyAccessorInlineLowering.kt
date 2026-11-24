/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.artifactConfigurations

class JsPropertyAccessorInlineLowering(
    val context: JsIrBackendContext
) : PropertyAccessorInlineLowering(context) {
    override fun IrProperty.isSafeToInline(accessContainer: IrDeclaration, callee: IrSimpleFunction): Boolean {
        if (!isSafeToInlineInClosedWorld())
            return false

        // These property accessors are handled as intrinsics by the codegen.
        if (symbol.owner.getter?.symbol == context.symbols.genericSharedVariableBox.load ||
            symbol.owner.setter?.symbol == context.symbols.genericSharedVariableBox.store
        ) {
            return false
        }

        // Member properties could be safely inlined, because initialization processed via parent declaration
        if (!isTopLevel && !context.incrementalCacheEnabled)
            return true

        // Just undefined value
        if (symbol == context.symbols.void) {
            return true
        }

        // In per-class mode, a use site inside a class slice is emitted into an artifact separate
        // from the rest artifact holding the accessed top-level property. An inlined accessor emits
        // a raw field reference that cannot cross an artifact boundary, while an accessor call is
        // an ordinary import. Use sites inside eager initializers are kept as accessor calls too.
        // Getter is safe to lower into backing field's export/import directly, so it is always
        // lowered in all granularities.
        if (
            callee == setter &&
            isTopLevel &&
            context.configuration.artifactConfigurations.any { it.granularity == JsGenerationGranularity.PER_CLASS } &&
            accessContainer.isEmittedIntoSeparateArtifact()
        ) {
            return false
        }

        // TODO: teach the deserializer to load constant property initializers
        val accessFile = accessContainer.fileOrNull ?: return false
        val file = fileOrNull ?: return false

        return accessFile == file
    }

    private fun IrDeclaration.isEmittedIntoSeparateArtifact(): Boolean {
        val eagerInitialization = context.propertyLazyInitialization.eagerInitialization
        var current: IrDeclaration? = this
        while (current != null) {
            if (current is IrClass) return true
            if (current is IrField && current.correspondingPropertySymbol?.owner?.hasAnnotation(eagerInitialization) == true) return true
            current = current.parent as? IrDeclaration
        }
        return false
    }
}
