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
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.config.artifactConfigurations

class JsPropertyAccessorInlineLowering(
    val context: JsIrBackendContext
) : PropertyAccessorInlineLowering(context) {
    override fun IrProperty.isSafeToInline(accessContainer: IrDeclaration): Boolean {
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

        // In per-class granularity, declarations of a class are emitted into an artifact separate from the file's
        // rest artifact where the accessed top-level property resides, and so is an initializer of an eagerly
        // initialized property. An inlined accessor would produce a field reference crossing artifact boundaries,
        // while an accessor call is a routine cross-artifact import: keep inlining only for use sites that are
        // known to end up in the rest artifact together with the property.
        if (
            isTopLevel &&
            context.configuration.artifactConfigurations.any { it.granularity == JsGenerationGranularity.PER_CLASS } &&
            accessContainer.isInsideSeparateArtifactInPerClassMode()
        ) {
            return false
        }

        // TODO: teach the deserializer to load constant property initializers
        val accessFile = accessContainer.fileOrNull ?: return false
        val file = fileOrNull ?: return false

        return accessFile == file
    }

    private fun IrDeclaration.isInsideSeparateArtifactInPerClassMode(): Boolean {
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
