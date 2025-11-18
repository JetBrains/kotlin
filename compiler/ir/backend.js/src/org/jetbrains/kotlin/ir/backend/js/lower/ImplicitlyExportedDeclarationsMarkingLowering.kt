/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.exportedVisibility
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
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
class ImplicitlyExportedDeclarationsMarkingLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val strictImplicitExport = context.configuration.getBoolean(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT)
    private val jsExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.symbols.jsExportAnnotationSymbol.constructors.single() }
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) { context.symbols.jsImplicitExportAnnotationSymbol.constructors.single() }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!declaration.isExported(context)) return null

        val implicitlyExportedDeclarations = when (declaration) {
            is IrFunction -> declaration.collectImplicitlyExportedDeclarations()
            is IrClass -> declaration.collectImplicitlyExportedDeclarations()
            is IrProperty -> declaration.collectImplicitlyExportedDeclarations()
            else -> emptySet()
        }

        implicitlyExportedDeclarations.forEach { it.markWithJsImplicitExportOrUpgrade() }

        return null
    }

    private fun IrClass.collectImplicitlyExportedDeclarations(): Set<IrDeclaration> {
        return typeParameters.asSequence()
            .flatMap { it.superTypes }
            .distinct()
            .flatMap { it.collectImplicitlyExportedDeclarations() }
            .toSet()
    }


    private fun IrFunction.collectImplicitlyExportedDeclarations(): Set<IrDeclaration> {
        val types = buildSet {
            add(returnType)
            if (this@collectImplicitlyExportedDeclarations !is IrConstructor || exportedVisibility != ExportedVisibility.PRIVATE) {
                // We don't export parameters of private constructors
                nonDispatchParameters.mapTo(this) { it.type }
            }
            typeParameters.flatMapTo(this) { it.superTypes }
        }

        return types.flatMap { it.collectImplicitlyExportedDeclarations(includeArguments = true) }.toSet()
    }

    private fun IrProperty.collectImplicitlyExportedDeclarations(): Set<IrDeclaration> {
        val getterImplicitlyExportedDeclarations = getter?.collectImplicitlyExportedDeclarations() ?: emptySet()
        val setterImplicitlyExportedDeclarations = setter?.collectImplicitlyExportedDeclarations() ?: emptySet()
        val fieldImplicitlyExportedDeclarations = backingField?.type?.collectImplicitlyExportedDeclarations(includeArguments = true) ?: emptySet()

        return getterImplicitlyExportedDeclarations + setterImplicitlyExportedDeclarations + fieldImplicitlyExportedDeclarations
    }

    private fun IrType.collectImplicitlyExportedDeclarations(includeArguments: Boolean = false): Set<IrDeclaration> {
        if (this is IrDynamicType || this !is IrSimpleType)
            return emptySet()

        val nonNullType = makeNotNull() as IrSimpleType
        val classifier = nonNullType.classifier

        return when {
            nonNullType.isPrimitiveType() ||
                    nonNullType.isPrimitiveArray() ||
                    nonNullType.isAny() ||
                    nonNullType.isNothing() ||
                    nonNullType.isUnit()
                -> emptySet()

            classifier is IrTypeParameterSymbol -> classifier.owner.superTypes
                .flatMap { it.collectImplicitlyExportedDeclarations() }
                .toSet()

            classifier is IrClassSymbol -> {
                val klass = classifier.owner
                val result = mutableSetOf<IrDeclaration>()

                val isSpeciallyExportedType = nonNullType.isSpeciallyExportedType()

                if (!isSpeciallyExportedType && klass.shouldBeMarkedWithImplicitExportOrUpgraded()) {
                    result.add(klass)
                }

                if (includeArguments && (isSpeciallyExportedType || klass.isExternal || klass.couldBeConvertedToExplicitExport() == true || klass.isExported(context))) {
                    arguments.flatMapTo(result) {
                        when (it) {
                            is IrStarProjection -> emptySet()
                            is IrTypeProjection -> it.type.collectImplicitlyExportedDeclarations()
                        }
                    }
                }

                result
            }

            else -> emptySet()
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
                    JsIrBuilder.buildConstructorCall(jsExportCtor)
                } else it
            }
        } else if (strictImplicitExport) {
            annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsImplicitExportCtor).apply {
                arguments[0] = false.toIrConst(context.irBuiltIns.booleanType)
            }

            parentClassOrNull?.takeIf { it.shouldBeMarkedWithImplicitExportOrUpgraded() }?.markWithJsImplicitExportOrUpgrade()
        }
    }
}
