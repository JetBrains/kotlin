/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.correspondingStatic
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.transformMemberToStaticFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

val EXPORTED_DEFAULT_IMPLEMENTATIONS by IrDeclarationOriginImpl.Regular

val IrDeclaration.isExportedDefaultImplementation: Boolean
    get() = origin == EXPORTED_DEFAULT_IMPLEMENTATIONS

/**
 * Extracts default implementations from exported interfaces
 */
@PhasePrerequisites(PrepareCollectionsToExportLowering::class)
class PrepareExportedDefaultImplementationsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val allowImplementingInterfaces = context.configuration.languageVersionSettings.supportsFeature(
        LanguageFeature.JsExportInterfacesInImplementableWay
    )

    private fun IrOverridableDeclaration<*>.shouldExtractAsExportedDefaultImplementation(): Boolean =
        origin != CONVERTERS_TO_JS_COLLECTIONS
                && modality != Modality.ABSTRACT
                && isReal
                && isExported(context)
                && parentClassOrNull?.kind?.isInterface == true
                && (this !is IrSimpleFunction || dispatchReceiverParameter != null && correspondingPropertySymbol == null)

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!allowImplementingInterfaces) return null

        return when (declaration) {
            is IrSimpleFunction ->
                runIf(declaration.shouldExtractAsExportedDefaultImplementation()) {
                    listOf(declaration, declaration.createCorrespondingStaticImplementationAndModifyCurrent())
                }

            is IrProperty ->
                runIf(declaration.shouldExtractAsExportedDefaultImplementation()) {
                    val correspondingDefaultImplementationProperty = declaration.copyAsExportedDefaultImplementationProperty()

                    correspondingDefaultImplementationProperty.getter =
                        declaration.getter?.createCorrespondingStaticImplementationAndModifyCurrent()
                            ?.also { it.correspondingPropertySymbol = correspondingDefaultImplementationProperty.symbol }

                    correspondingDefaultImplementationProperty.setter =
                        declaration.setter?.createCorrespondingStaticImplementationAndModifyCurrent()
                            ?.also { it.correspondingPropertySymbol = correspondingDefaultImplementationProperty.symbol }

                    listOf(declaration, correspondingDefaultImplementationProperty)
                }

            else -> null
        }
    }

    private fun IrSimpleFunction.createCorrespondingStaticImplementationAndModifyCurrent() =
        with(context) {
            transformMemberToStaticFunction(this@createCorrespondingStaticImplementationAndModifyCurrent)
                .apply {
                    origin = EXPORTED_DEFAULT_IMPLEMENTATIONS
                    modality = Modality.FINAL
                }
                .also {
                    correspondingStatic = it
                    body = context.createIrBuilder(symbol).irBlockBody(this@createCorrespondingStaticImplementationAndModifyCurrent) {
                        +irReturn(
                            irCall(it.symbol).apply {
                                parameters.forEachIndexed { index, irValueParameter ->
                                    arguments[index] = irGet(irValueParameter)
                                }
                                (parentAsClass.typeParameters + typeParameters).forEachIndexed { index, irTypeParameter ->
                                    typeArguments[index] = irTypeParameter.defaultType
                                }
                            })
                    }
                }
        }

    private fun IrProperty.copyAsExportedDefaultImplementationProperty(): IrProperty {
        val originalProperty = this
        return context.irFactory.createProperty(
            originalProperty.startOffset,
            originalProperty.endOffset,
            EXPORTED_DEFAULT_IMPLEMENTATIONS,
            originalProperty.name,
            originalProperty.visibility,
            Modality.FINAL,
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
        }
    }
}
