/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateFunction() {

    val returnTypeFieldField = Field("returnTypeField", IrType)
    val typeParametersField = Field("typeParameters", +"List<" + IrTypeParameter + ">")
    val dispatchReceiverParameterField = Field("dispatchReceiverParameter", IrValueParameter + "?")
    val extensionReceiverParameterField = Field("extensionReceiverParameter", IrValueParameter + "?")
    val valueParametersField = Field("valueParameters", +"List<" + IrValueParameter + ">")
    val bodyField = Field("body", IrBody + "?")
    val metadataField = Field("metadata", MetadataSource + "?")
    val visibilityField = Field("visibility", DescriptorVisibility)
    val overriddenSymbolsField = Field("overriddenSymbols", +"List<" + irSymbol("IrSimpleFunctionSymbol") + ">")
    val attributeOwnerIdField = Field("attributeOwnerId", IrAttributeContainer)
    val correspondingPropertySymbolField = Field("correspondingPropertySymbol", IrPropertySymbol + "?")

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
                valueParametersField.toPersistentField(+"emptyList()"),
                bodyField.toBody(),
                metadataField.toPersistentField(+"null"),
                visibilityField.toPersistentField(+"visibility"),
                overriddenSymbolsField.toPersistentField(+"emptyList()"),
                lines(
                    +"@Suppress(\"LeakingThis\")",
                    attributeOwnerIdField.toPersistentField(+"this"),
                ),
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
            metadataField,
            visibilityField,
            typeParametersField,
            valueParametersField,
            correspondingPropertySymbolField,
            overriddenSymbolsField,
            attributeOwnerIdField,
        )()
    })
}