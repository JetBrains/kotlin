/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.prependFunctionCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.name.Name

private var IrFile.initializationFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)
private var IrFile.isPureForInitialization: Boolean? by irAttribute(copyByDefault = false)

abstract class PropertyLazyInitLowering(
    private val context: JsCommonBackendContext
) : BodyLoweringPass {

    private val irBuiltIns
        get() = context.irBuiltIns

    protected abstract val initializationGenerator: LazyGlobalInitializationGenerator

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.propertyLazyInitialization.enabled) {
            return
        }

        if (container !is IrField && container !is IrSimpleFunction && container !is IrProperty)
            return

        if (!container.isCompatibleDeclaration(context)) return

        val file = container.parent as? IrFile
            ?: return

        val initFun = file.initializationFunction ?: when {
            file.isPureForInitialization == true -> null
            else -> {
                createInitializationFunction(file).also {
                    file.initializationFunction = it
                }
            }
        } ?: return

        val initializationCall = JsIrBuilder.buildCall(
            target = initFun.symbol,
            type = initFun.returnType,
            origin = PROPERTY_INIT_FUN_CALL
        )

        if (container is IrSimpleFunction) irBody.prependFunctionCall(initializationCall)
    }

    private fun createInitializationFunction(
        file: IrFile
    ): IrSimpleFunction? {
        val fileName = file.name

        val declarations = file.declarations.toList()

        val fieldToInitializer = calculateFieldToExpression(
            declarations,
            context
        )

        if (fieldToInitializer.isEmpty()) return null

        val allFieldsInFilePure = allFieldsInFilePure(fieldToInitializer.values)
        file.isPureForInitialization = allFieldsInFilePure
        if (allFieldsInFilePure) {
            return null
        }

        val initializedField = initializationGenerator.createStateField(
            name = Name.identifier("properties initialized $fileName"),
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION,
        ).apply {
            file.declarations.add(this)
            parent = file
        }

        val statements = buildList<IrStatement> {
            fieldToInitializer.forEach { [field, expression] ->
                add(createIrSetField(field, expression))
            }
            add(JsIrBuilder.buildBlock(irBuiltIns.unitType))
        }

        return initializationGenerator.createStaticInitFunction(
            name = Name.special("<init properties $fileName>"),
            klass = null,
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION,
            stateField = initializedField,
            initializers = statements,
        ).apply {
            file.declarations.add(this)
            parent = file
        }
    }

    companion object {
        val PROPERTY_INIT_FUN_CALL by IrStatementOriginImpl
    }
}

class JsPropertyLazyInitLowering(context: JsIrBackendContext) : PropertyLazyInitLowering(context) {
    override val initializationGenerator = JsLazyGlobalInitializationGenerator(context)
}

private fun createIrSetField(field: IrField, expression: IrExpression): IrSetField {
    return JsIrBuilder.buildSetField(
        symbol = field.symbol,
        receiver = null,
        value = expression,
        type = expression.type
    )
}

private fun allFieldsInFilePure(fieldToInitializer: Collection<IrExpression>): Boolean =
    fieldToInitializer
        .all { expression ->
            expression.isPure(anyVariable = true)
        }

/**
 * Removes property initializers if they were initialized lazily.
 */
class RemoveInitializersForLazyProperties(
    private val context: JsCommonBackendContext
) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.propertyLazyInitialization.enabled) {
            return null
        }

        if (declaration !is IrField) return null

        if (!declaration.isCompatibleDeclaration(context)) return null

        val file = declaration.parent as? IrFile ?: return null

        if (file.isPureForInitialization == true) return null

        val allFieldsInFilePure = file.isPureForInitialization
            ?: calculateFileFieldsPureness(file)

        if (allFieldsInFilePure) {
            return null
        }

        declaration.correspondingProperty
            ?.takeIf { it.isForLazyInit() }
            ?.backingField
            ?.let {
                it.initializer = null
            }

        return null
    }

    private fun calculateFileFieldsPureness(file: IrFile): Boolean {
        val declarations = file.declarations.toList()
        val expressions = calculateFieldToExpression(declarations, context)
            .values

        val allFieldsInFilePure = allFieldsInFilePure(expressions)
        file.isPureForInitialization = allFieldsInFilePure
        return allFieldsInFilePure
    }
}

private fun calculateFieldToExpression(
    declarations: Collection<IrDeclaration>,
    context: JsCommonBackendContext
): Map<IrField, IrExpression> =
    declarations
        .asSequence()
        .filter { it.isCompatibleDeclaration(context) }
        .map { it.correspondingProperty }
        .filterNotNull()
        .filter { it.isForLazyInit() }
        .distinct()
        .mapNotNull { it.backingField }
        .filter { it.initializer != null }
        .map { it to it.initializer!!.expression }
        .toMap()

private fun IrProperty.isForLazyInit() = isTopLevel && !isConst

private val IrDeclaration.correspondingProperty: IrProperty?
    get() {
        if (this !is IrSimpleFunction && this !is IrField && this !is IrProperty)
            return null

        return when (this) {
            is IrProperty -> this
            is IrSimpleFunction -> propertyWithPersistentSafe {
                correspondingPropertySymbol?.owner
            }
            is IrField -> propertyWithPersistentSafe {
                correspondingPropertySymbol?.owner
            }
            else -> compilationException(
                "Can be only IrProperty, IrSimpleFunction or IrField",
                this
            )
        }
    }

private fun IrDeclaration.propertyWithPersistentSafe(transform: IrDeclaration.() -> IrProperty?): IrProperty? =
    withPersistentSafe(transform)

private fun <T> IrDeclaration.withPersistentSafe(transform: IrDeclaration.() -> T?): T? =
    transform()

private fun IrDeclaration.isCompatibleDeclaration(context: JsCommonBackendContext) =
    correspondingProperty?.let {
        !it.isExternal && it.isForLazyInit() && !it.hasAnnotation(context.propertyLazyInitialization.eagerInitialization)
    } ?: true && withPersistentSafe { origin in compatibleOrigins } == true

private val compatibleOrigins = listOf(
    IrDeclarationOrigin.DEFINED,
    IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
    IrDeclarationOrigin.PROPERTY_DELEGATE,
    IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
    IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
)
