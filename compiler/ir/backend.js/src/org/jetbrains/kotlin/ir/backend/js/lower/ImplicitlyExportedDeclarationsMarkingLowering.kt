/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lazy2
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.couldBeConvertedToExplicitExport
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

class ImplicitlyExportedDeclarationsMarkingLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val strictImplicitExport = context.configuration.getBoolean(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT)
    private val jsExportCtor by context.lazy2 { context.intrinsics.jsExportAnnotationSymbol.constructors.single() }
    private val jsImplicitExportCtor by context.lazy2 { context.intrinsics.jsImplicitExportAnnotationSymbol.constructors.single() }

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
            addAll(valueParameters.map { it.type })
            addAll(typeParameters.flatMap { it.superTypes })
        }

        return types.flatMap { it.collectImplicitlyExportedDeclarations() }.toSet()
    }

    private fun IrProperty.collectImplicitlyExportedDeclarations(): Set<IrDeclaration> {
        val getterImplicitlyExportedDeclarations = getter?.collectImplicitlyExportedDeclarations() ?: emptySet()
        val setterImplicitlyExportedDeclarations = setter?.collectImplicitlyExportedDeclarations() ?: emptySet()
        val fieldImplicitlyExportedDeclarations = backingField?.type?.collectImplicitlyExportedDeclarations() ?: emptySet()

        return getterImplicitlyExportedDeclarations + setterImplicitlyExportedDeclarations + fieldImplicitlyExportedDeclarations
    }

    private fun IrType.collectImplicitlyExportedDeclarations(): Set<IrDeclaration> {
        if (this is IrDynamicType || this !is IrSimpleType)
            return emptySet()

        val nonNullType = makeNotNull() as IrSimpleType
        val classifier = nonNullType.classifier

        return when {
            nonNullType.isPrimitiveType() || nonNullType.isPrimitiveArray() || nonNullType.isAny() || nonNullType.isUnit() -> emptySet()
            classifier is IrTypeParameterSymbol -> classifier.owner.superTypes.flatMap { it.collectImplicitlyExportedDeclarations() }
                .toSet()

            classifier is IrClassSymbol -> setOfNotNull(classifier.owner.takeIf { it.shouldBeMarkedWithImplicitExportOrUpgraded() })
            else -> emptySet()
        }
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
                putValueArgument(0, false.toIrConst(context.irBuiltIns.booleanType))
            }

            parentClassOrNull?.takeIf { it.shouldBeMarkedWithImplicitExportOrUpgraded() }?.markWithJsImplicitExportOrUpgrade()
        }
    }
}