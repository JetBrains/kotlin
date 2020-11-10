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
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isPure
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrElementBase
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.DeclarationCarrier
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

    val fileToInitialisationFuns
        get() = context.fileToInitialisationFuns

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val property = container.correspondingProperty ?: return

        val topLevelProperty = property
            .takeIf { it.isForLazyInit() }
            ?.takeIf { it.backingField?.initializer != null }
            ?: return

        val file = topLevelProperty.parent as? IrFile
            ?: return

        val initFun = fileToInitialisationFuns[file]
            ?: createInitialisationFunction(file)?.also { fileToInitialisationFuns[file] = it }
            ?: return

        val initialisationCall = JsIrBuilder.buildCall(
            target = initFun.symbol,
            type = initFun.returnType
        )

        when (container) {
            is IrSimpleFunction ->
                irBody.addInitialisation(initialisationCall, container)
            is IrField -> {
                property
                    .let { listOf(it.getter, it.setter) }
                    .filterNotNull()
                    .forEach {
                        irBody.addInitialisation(initialisationCall, it)
                    }
            }
        }
    }

    private fun createInitialisationFunction(
        file: IrFile
    ): IrSimpleFunction? {
        val fileName = file.name

        val declarations = ArrayList(file.declarations)

        val fieldToInitializer = calculateFieldToExpression(
            declarations
        )

//        if (allFieldsInFilePure(fieldToInitializer.values)) {
//            return null
//        }

        val initialisedField = irFactory.createInitialisationField(fileName)
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
                initialisedField
            )
        }
    }

    private fun IrFactory.createInitialisationField(fileName: String): IrField =
        buildField {
            name = Name.identifier("properties initialised $fileName")
            type = irBuiltIns.booleanType
            isStatic = true
            isFinal = true
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

    private fun IrSimpleFunction.buildPropertiesInitializationBody(
        initializers: Map<IrField, IrExpression>,
        initialisedField: IrField
    ) {
        body = irFactory.createBlockBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            buildBodyWithIfGuard(initializers, initialisedField)
        )
    }

    private fun buildBodyWithIfGuard(
        initializers: Map<IrField, IrExpression>,
        initialisedField: IrField
    ): List<IrStatement> {
        val statements = initializers
            .map { (field, expression) ->
                createIrSetField(field, expression)
            }

        val upGuard = createIrSetField(
            initialisedField,
            JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
        )

        return JsIrBuilder.buildIfElse(
            type = irBuiltIns.unitType,
            cond = calculator.not(createIrGetField(initialisedField)),
            thenBranch = JsIrBuilder.buildComposite(
                type = irBuiltIns.unitType,
                statements = mutableListOf(upGuard).apply { addAll(statements) }
            )
        ).let { listOf(it) }
    }
}

private fun IrBody.addInitialisation(
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

    val fileToInitialisationFuns
        get() = context.fileToInitialisationFuns

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
//        val file = declaration.parent as? IrFile ?: return null
//        val declarations = ArrayList(file.declarations)
//
//        val fields = calculateFieldToExpression(declarations).values
//
//        if (allFieldsInFilePure(fields)) {
//            return null
//        }

        declaration.correspondingProperty
            ?.takeIf { it.isForLazyInit() }
            ?.backingField
            ?.let { it.initializer = null }

        return null
    }
}

private fun calculateFieldToExpression(declarations: Collection<IrDeclaration>): Map<IrField, IrExpression> =
    declarations
        .asSequence()
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

        // Objects are in fact fields and we need to ignore them
        val originField = (this as? DeclarationCarrier)?.originField
        if (originField == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION || originField == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE)
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
    transform()