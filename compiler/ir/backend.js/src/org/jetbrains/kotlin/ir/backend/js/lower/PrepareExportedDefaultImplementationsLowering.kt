/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.correspondingStatic
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.transformMemberToStaticFunction
import org.jetbrains.kotlin.ir.backend.js.utils.isExportedInterface
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.util.parentClassOrNull

val EXPORTED_DEFAULT_IMPLEMENTATIONS by IrDeclarationOriginImpl.Regular

val IrDeclaration.isDefaultImplementation: Boolean
    get() = origin == EXPORTED_DEFAULT_IMPLEMENTATIONS

private var IrProperty.holderOfAccessorsWithDefaultImplementations: IrProperty? by irAttribute(copyByDefault = false)

/**
 * Extracts default implementations from exported interfaces
 */
class PrepareExportedDefaultImplementationsLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    private fun IrSimpleFunction.shouldExtractAsExportedDefaultImplementation(): Boolean =
        origin != CONVERTERS_TO_JS_COLLECTIONS
                && modality != Modality.ABSTRACT
                && overriddenSymbols.isEmpty()
                && dispatchReceiverParameter != null
                && (correspondingPropertySymbol?.owner ?: this).isExported(context)
                && parentClassOrNull?.kind?.isInterface == true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return (declaration as? IrSimpleFunction)
            ?.takeIf { it.shouldExtractAsExportedDefaultImplementation() }
            ?.let { memberFunction ->
                with(context) {
                    transformMemberToStaticFunction(memberFunction)
                        .apply { origin = EXPORTED_DEFAULT_IMPLEMENTATIONS }
                        .also { staticFunction ->
                            memberFunction.body = null
                            memberFunction.correspondingStatic = staticFunction
                            memberFunction.correspondingPropertySymbol?.let {
                                val property = it.owner
                                val copy = property.copyAsExportedProperty()
                                staticFunction.correspondingPropertySymbol = copy.symbol
                                if (property.getter === memberFunction) {
                                    copy.getter = staticFunction
                                } else {
                                    copy.setter = staticFunction
                                }
                            }
                        }
                        .let { listOf(declaration, it) }
                }
            }
    }

    private fun IrProperty.copyAsExportedProperty(): IrProperty {
        val originalProperty = this
        return originalProperty.holderOfAccessorsWithDefaultImplementations ?: context.irFactory.createProperty(
            originalProperty.startOffset,
            originalProperty.endOffset,
            EXPORTED_DEFAULT_IMPLEMENTATIONS,
            originalProperty.name,
            originalProperty.visibility,
            originalProperty.modality,
            IrPropertySymbolImpl(),
            originalProperty.isVar,
            originalProperty.isConst,
            originalProperty.isLateinit,
            originalProperty.isDelegated,
            originalProperty.isExternal,
            originalProperty.containerSource,
            originalProperty.isExpect,
            originalProperty.isFakeOverride
        ).apply {
            copyAttributes(originalProperty)
            annotations = originalProperty.annotations
            parent = originalProperty.parent
        }.also { originalProperty.holderOfAccessorsWithDefaultImplementations = it }
    }
}
