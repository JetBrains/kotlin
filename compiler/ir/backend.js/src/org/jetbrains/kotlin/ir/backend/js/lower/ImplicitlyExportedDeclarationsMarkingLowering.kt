/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.exportedVisibility
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.PrepareSuspendFunctionsForExportLowering
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedVisibility
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.couldBeConvertedToExplicitExport
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

/**
 * Adds `@JsImplicitExport` annotation to declarations which are not exported but are used inside other exported declarations as a type.
 */
@PhasePrerequisites(PrepareSuspendFunctionsForExportLowering::class)
class ImplicitlyExportedDeclarationsMarkingLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val strictImplicitExport = context.configuration.getBoolean(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT)
    private val jsExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.symbols.jsExportAnnotationSymbol.constructors.single() }
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.symbols.jsImplicitExportAnnotationSymbol.constructors.single() }

    private var pendingTransitivelyExportedClasses: MutableSet<IrClass> = hashSetOf()

    override fun lower(irModule: IrModuleFragment) {
        super.lower(irModule)
        while (pendingTransitivelyExportedClasses.isNotEmpty()) {
            for (klass in pendingTransitivelyExportedClasses) {
                klass.markWithJsImplicitExportOrUpgrade()
            }
            val currentlyBeingProcessed = pendingTransitivelyExportedClasses
            pendingTransitivelyExportedClasses = hashSetOf()
            for (declaration in currentlyBeingProcessed) {
                declaration.collectImplicitlyExportedDeclarations()
            }
            pendingTransitivelyExportedClasses.removeAll(currentlyBeingProcessed)
        }
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (context.incrementalCacheEnabled) {
            // With enabled incremental compilation, we always treat @JsImplicitExport as a regular @JsExport.
            // This is because adding or removing a reference to an implicitly exported declaration doesn't trigger its recompilation,
            // which can lead to issues like KT-70622.
            if (declaration.couldBeConvertedToExplicitExport() == true) {
                declaration.markWithJsImplicitExportOrUpgrade()
            }
            return null
        }
        if (!declaration.isExported(context)) return null

        when (declaration) {
            is IrFunction -> declaration.collectImplicitlyExportedDeclarations()
            is IrClass -> declaration.collectImplicitlyExportedDeclarations()
            is IrProperty -> declaration.collectImplicitlyExportedDeclarations()
        }

        return null
    }

    private fun IrClass.collectImplicitlyExportedDeclarations() {
        typeParameters.asSequence()
            .flatMap { it.superTypes }
            .distinct()
            .forEach { it.collectImplicitlyExportedDeclarations() }

        superTypes
            .forEach { it.collectImplicitlyExportedDeclarations() }
    }


    private fun IrFunction.collectImplicitlyExportedDeclarations() {
        val types = buildSet {
            add(returnType)
            if (this@collectImplicitlyExportedDeclarations !is IrConstructor || exportedVisibility != ExportedVisibility.PRIVATE) {
                // We don't export parameters of private constructors
                nonDispatchParameters.mapTo(this) { it.type }
            }
            typeParameters.flatMapTo(this) { it.superTypes }
        }

        types.forEach { it.collectImplicitlyExportedDeclarations(includeArguments = true) }
    }

    private fun IrProperty.collectImplicitlyExportedDeclarations() {
        getter?.collectImplicitlyExportedDeclarations()
        setter?.collectImplicitlyExportedDeclarations()
        backingField?.type?.collectImplicitlyExportedDeclarations(includeArguments = true)
    }

    private fun IrType.collectImplicitlyExportedDeclarations(includeArguments: Boolean = false) {
        if (this is IrDynamicType || this !is IrSimpleType)
            return

        val nonNullType = makeNotNull() as IrSimpleType
        val classifier = nonNullType.classifier

        when {
            nonNullType.isPrimitiveType() ||
                    nonNullType.isPrimitiveArray() ||
                    nonNullType.isAny() ||
                    nonNullType.isNothing() ||
                    nonNullType.isUnit()
                -> return

            classifier is IrTypeParameterSymbol -> classifier.owner.superTypes
                .forEach { it.collectImplicitlyExportedDeclarations() }

            classifier is IrClassSymbol -> {
                val klass = classifier.owner

                val isSpeciallyExportedType = nonNullType.isSpeciallyExportedType()

                if (!isSpeciallyExportedType && klass.shouldBeMarkedWithImplicitExportOrUpgraded()) {
                    pendingTransitivelyExportedClasses.add(klass)
                }

                if (includeArguments && (isSpeciallyExportedType || klass.isExternal || klass.couldBeConvertedToExplicitExport() == true || klass.isExported(context))) {
                    arguments.forEach {
                        when (it) {
                            is IrStarProjection -> {}
                            is IrTypeProjection -> it.type.collectImplicitlyExportedDeclarations()
                        }
                    }
                }
            }
        }
    }

    private fun IrSimpleType.isSpeciallyExportedType(): Boolean {
        return isFunction() || isThrowable() || isArray()
    }

    private fun IrDeclaration.shouldBeMarkedWithImplicitExportOrUpgraded(): Boolean {
        return this is IrClass && !isExternal && !isExported(context)
    }

    private fun IrDeclaration.markWithJsImplicitExportOrUpgrade() {
        if (couldBeConvertedToExplicitExport() == true) {
            annotations = annotations.memoryOptimizedMap {
                if (it.isAnnotation(JsAnnotations.jsImplicitExportFqn)) {
                    JsIrBuilder.buildAnnotation(jsExportCtor)
                } else it
            }
        } else if (strictImplicitExport) {
            annotations = annotations memoryOptimizedPlus JsIrBuilder.buildAnnotation(jsImplicitExportCtor).apply {
                arguments[0] = false.toIrConst(context.irBuiltIns.booleanType)
            }

            parentClassOrNull?.takeIf { it.shouldBeMarkedWithImplicitExportOrUpgraded() }?.markWithJsImplicitExportOrUpgrade()
        }
    }
}
