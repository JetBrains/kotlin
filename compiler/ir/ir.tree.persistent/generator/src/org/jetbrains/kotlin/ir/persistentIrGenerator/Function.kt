/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateFunction() {

    val returnTypeFieldField = Field("returnTypeField", IrType, typeProto)
    val bodyField = Field("body", IrBody + "?", bodyProto)
    val visibilityField = Field("visibility", DescriptorVisibility, visibilityProto)
    val overriddenSymbolsField = Field("overriddenSymbols", +"List<" + IrSimpleFunctionSymbol + ">", simpleFunctionSymbolListProto)
    val correspondingPropertySymbolField = Field("correspondingPropertySymbol", IrPropertySymbol + "?", propertySymbolProto)

    writeFile("PersistentIrFunctionCommon.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal abstract class PersistentIrFunctionCommon(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                name,
                visibility,
                returnType,
                isInline,
                isExternal,
                +"override val isTailrec: Boolean",
                +"override val isSuspend: Boolean",
                +"override val isOperator: Boolean",
                +"override val isInfix: Boolean",
                isExpect,
                containerSource + " = null",
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("Function", baseClass = "IrSimpleFunction") + " " + blockSpaced(
                commonFields,
                returnTypeFieldField.toPersistentField(+"returnType", modifier = "private"),
                lines(
                    +"final override var returnType: IrType",
                    lines(
                        +"get() = returnTypeField.let " + block(
                            +"if (it !== " + import(
                                "IrUninitializedType",
                                "org.jetbrains.kotlin.ir.types.impl"
                            ) + ") it else throw " + import(
                                "ReturnTypeIsNotInitializedException",
                                "org.jetbrains.kotlin.ir.types.impl"
                            ) + "(this)"
                        ),
                        +"set(c) " + block(
                            +"returnTypeField = c"
                        )
                    ).indent()
                ),
                typeParametersField.toPersistentField(+"emptyList()"),
                dispatchReceiverParameterField.toPersistentField(+"null"),
                extensionReceiverParameterField.toPersistentField(+"null"),
                contextReceiverParametersCount,
                valueParametersField.toPersistentField(+"emptyList()"),
                bodyField.toBody(),
                +"override var metadata: " + MetadataSource + "? = null",
                visibilityField.toPersistentField(+"visibility"),
                overriddenSymbolsField.toPersistentField(+"emptyList()"),
                +"override var attributeOwnerId: " + IrAttributeContainer + " = this",
                correspondingPropertySymbolField.toPersistentField(+"null"),
            ),
            id,
        )()
    })

    writeFile("carriers/FunctionCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "Function",
            returnTypeFieldField,
            dispatchReceiverParameterField,
            extensionReceiverParameterField,
            bodyField,
            visibilityField,
            typeParametersField,
            valueParametersField,
            correspondingPropertySymbolField,
            overriddenSymbolsField,
        )()
    })

    addCarrierProtoMessage(
        "Function",
        returnTypeFieldField,
        dispatchReceiverParameterField,
        extensionReceiverParameterField,
        bodyField,
        visibilityField,
        typeParametersField,
        valueParametersField,
        correspondingPropertySymbolField,
        overriddenSymbolsField,
    )
}