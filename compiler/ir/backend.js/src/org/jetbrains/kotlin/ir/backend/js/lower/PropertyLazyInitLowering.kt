/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.util.isPure
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrElementBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.name.Name
import kotlin.collections.component1
import kotlin.collections.component2

class PropertyLazyInitLowering(
    private val context: JsIrBackendContext
) : BodyLoweringPass {

    private val irBuiltIns
        get() = context.irBuiltIns

    private val calculator = JsIrArithBuilder(context)

    private val irFactory
        get() = context.irFactory

    private val fileToInitializationFuns
        get() = context.fileToInitializationFuns

    private val fileToInitializerPureness
        get() = context.fileToInitializerPureness

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.propertyLazyInitialization) {
            return
        }

        if (container !is IrField && container !is IrSimpleFunction && container !is IrProperty)
            return

        if (!container.isCompatibleDeclaration()) return

        val file = container.parent as? IrFile
            ?: return

        container.assertCompatibleDeclaration()

        val initFun = (when {
            file in fileToInitializationFuns -> fileToInitializationFuns[file]
            fileToInitializerPureness[file] == true -> null
            else -> {
                createInitializationFunction(file).also {
                    fileToInitializationFuns[file] = it
                }
            }
        }) ?: return

        val initializationCall = JsIrBuilder.buildCall(
            target = initFun.symbol,
            type = initFun.returnType
        )

        when (container) {
            is IrSimpleFunction ->
                irBody.addInitialization(initializationCall, container)
            is IrField -> {
                container
                    .correspondingProperty
                    ?.takeIf { it.isForLazyInit() }
                    ?.takeIf { it.backingField?.initializer != null }
                    ?.let { listOf(it.getter, it.setter) }
                    ?.filterNotNull()
                    ?.forEach {
                        irBody.addInitialization(initializationCall, it)
                    }
            }
        }
    }

    private fun createInitializationFunction(
        file: IrFile
    ): IrSimpleFunction? {
        val fileName = file.name

        val declarations = file.declarations.toList()

        val fieldToInitializer = calculateFieldToExpression(
            declarations
        )

        if (fieldToInitializer.isEmpty()) return null

        val allFieldsInFilePure = allFieldsInFilePure(fieldToInitializer.values)
        fileToInitializerPureness[file] = allFieldsInFilePure
        if (allFieldsInFilePure) {
            return null
        }

        val initializedField = irFactory.createInitializationField(fileName)
            .apply {
                file.declarations.add(this)
                parent = file
            }

        return irFactory.addFunction(file) {
            name = Name.identifier("init properties $fileName")
            returnType = irBuiltIns.unitType
            visibility = INTERNAL
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.apply {
            buildPropertiesInitializationBody(
                fieldToInitializer,
                initializedField
            )
        }
    }

    private fun IrFactory.createInitializationField(fileName: String): IrField =
        buildField {
            name = Name.identifier("properties initialized $fileName")
            type = irBuiltIns.booleanType
            isStatic = true
            isFinal = true
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

    private fun IrSimpleFunction.buildPropertiesInitializationBody(
        initializers: Map<IrField, IrExpression>,
        initializedField: IrField
    ) {
        body = irFactory.createBlockBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            buildBodyWithIfGuard(initializers, initializedField)
        )
    }

    private fun buildBodyWithIfGuard(
        initializers: Map<IrField, IrExpression>,
        initializedField: IrField
    ): List<IrStatement> {
        val statements = initializers
            .map { (field, expression) ->
                createIrSetField(field, expression)
            }

        val upGuard = createIrSetField(
            initializedField,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
        )

        return JsIrBuilder.buildIfElse(
            type = irBuiltIns.unitType,
            cond = calculator.not(createIrGetField(initializedField)),
            thenBranch = JsIrBuilder.buildComposite(
                type = irBuiltIns.unitType,
                statements = mutableListOf(upGuard).apply { addAll(statements) }
            )
        ).let { listOf(it) }
    }
}

private fun IrBody.addInitialization(
    initCall: IrCall,
    container: IrSimpleFunction
) {
    when (this) {
        is IrExpressionBody -> {
            expression = JsIrBuilder.buildComposite(
                type = container.returnType,
                statements = listOf(
                    initCall,
                    expression
                )
            )
        }
        is IrBlockBody -> {
            statements.add(
                0,
                initCall
            )
        }
    }
}

private fun createIrGetField(field: IrField): IrGetField {
    return JsIrBuilder.buildGetField(
        symbol = field.symbol,
        receiver = null
    )
}

private fun createIrSetField(field: IrField, expression: IrExpression): IrSetField {
    return JsIrBuilder.buildSetField(
        symbol = field.symbol,
        receiver = null,
        value = expression,
        type = expression.type
    )
}

private fun allFieldsInFilePure(fieldToInitializer: Collection<IrExpression>) =
    fieldToInitializer.all { it.isPure(anyVariable = true) }

class RemoveInitializersForLazyProperties(
    private val context: JsIrBackendContext
) : DeclarationTransformer {

    private val fileToInitializerPureness
        get() = context.fileToInitializerPureness

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.propertyLazyInitialization) {
            return null
        }

        if (declaration !is IrField) return null

        if (!declaration.isCompatibleDeclaration()) return null

        val file = declaration.parent as? IrFile ?: return null

        if (fileToInitializerPureness[file] == true) return null

        val allFieldsInFilePure = fileToInitializerPureness[file]
            ?: calculateFileFieldsPureness(file)

        if (allFieldsInFilePure) {
            return null
        }

        declaration.correspondingProperty
            ?.takeIf { it.isForLazyInit() }
            ?.backingField
            ?.let {
                it.assertCompatibleDeclaration()
                it.initializer = null
            }

        return null
    }

    private fun calculateFileFieldsPureness(file: IrFile): Boolean {
        val declarations = file.declarations.toList()
        val expressions = calculateFieldToExpression(declarations)
            .values

        val allFieldsInFilePure = allFieldsInFilePure(expressions)
        fileToInitializerPureness[file] = allFieldsInFilePure
        return allFieldsInFilePure
    }
}

private fun calculateFieldToExpression(declarations: Collection<IrDeclaration>): Map<IrField, IrExpression> =
    declarations
        .asSequence()
        .filter { it.isCompatibleDeclaration() }
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
            else -> error("Can be only IrProperty, IrSimpleFunction or IrField")
        }
    }

private fun IrDeclaration.propertyWithPersistentSafe(transform: IrDeclaration.() -> IrProperty?): IrProperty? =
    if (((this as? PersistentIrElementBase<*>)?.createdOn ?: 0) <= stageController.currentStage) {
        transform()
    } else null

private fun IrDeclaration.isCompatibleDeclaration() =
    origin in compatibleOrigins

private fun IrDeclaration.assertCompatibleDeclaration() {
    assert((this as? PersistentIrElementBase<*>)?.createdOn?.let { it == 0 } != false)
}

private val compatibleOrigins = listOf(
    IrDeclarationOrigin.DEFINED,
    IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
    IrDeclarationOrigin.PROPERTY_DELEGATE,
    IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
    IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
)