/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFile
import org.jetbrains.kotlin.backend.common.serialization.IrSymbolDeserializer
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinaryNameAndType
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.encodings.FunctionFlags
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty

internal fun ProtoClass.findClass(irClass: IrClass, fileReader: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer): ProtoClass {
    val signature = irClass.symbol.signature ?: error("No signature for ${irClass.render()}")
    var result: ProtoClass? = null

    for (i in 0 until this.declarationCount) {
        val child = this.getDeclaration(i)
        val childClass = when {
            child.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_CLASS -> child.irClass
            child.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_ENUM_ENTRY
                    && child.irEnumEntry.hasCorrespondingClass() -> child.irEnumEntry.correspondingClass
            else -> continue
        }

        val name = fileReader.string(childClass.name)
        if (name == irClass.name.asString()) {
            if (result == null)
                result = childClass
            else {
                val resultIdSignature = symbolDeserializer.deserializeIdSignature(BinarySymbolData.decode(result.base.symbol).signatureId)
                if (resultIdSignature == signature)
                    return result
                result = childClass
            }
        }
    }
    return result ?: error("Class ${irClass.render()} is not found")
}

internal fun ProtoClass.findProperty(irProperty: IrProperty, fileReader: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer): ProtoProperty {
    val signature = irProperty.symbol.signature ?: error("No signature for ${irProperty.render()}")
    var result: ProtoProperty? = null

    for (i in 0 until this.declarationCount) {
        val child = this.getDeclaration(i)
        if (child.declaratorCase != ProtoDeclaration.DeclaratorCase.IR_PROPERTY) continue
        val childProperty = child.irProperty

        val name = fileReader.string(child.irProperty.name)
        if (name == irProperty.name.asString()) {
            if (result == null)
                result = childProperty
            else {
                val resultIdSignature = symbolDeserializer.deserializeIdSignature(BinarySymbolData.decode(result.base.symbol).signatureId)
                if (resultIdSignature == signature)
                    return result
                result = childProperty
            }
        }
    }
    return result ?: error("Property ${irProperty.render()} is not found")
}

internal fun ProtoProperty.findAccessor(irProperty: IrProperty, irFunction: IrSimpleFunction): ProtoFunction {
    if (irFunction == irProperty.getter)
        return getter
    require(irFunction == irProperty.setter) { "Accessor should be either a getter or a setter. ${irFunction.render()}" }
    return setter
}

internal fun ProtoClass.findInlineFunction(irFunction: IrFunction, fileReader: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer): ProtoFunction {
    (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.let { irProperty ->
        return findProperty(irProperty, fileReader, symbolDeserializer).findAccessor(irProperty, irFunction)
    }

    val signature = irFunction.symbol.signature ?: error("No signature for ${irFunction.render()}")
    var result: ProtoFunction? = null
    for (i in 0 until this.declarationCount) {
        val child = this.getDeclaration(i)
        if (child.declaratorCase != ProtoDeclaration.DeclaratorCase.IR_FUNCTION) continue
        val childFunction = child.irFunction
        if (childFunction.base.valueParameterCount != irFunction.valueParameters.size) continue
        if (childFunction.base.hasExtensionReceiver() xor (irFunction.extensionReceiverParameter != null)) continue
        if (childFunction.base.hasDispatchReceiver() xor (irFunction.dispatchReceiverParameter != null)) continue
        if (!FunctionFlags.decode(childFunction.base.base.flags).isInline) continue

        val nameAndType = BinaryNameAndType.decode(childFunction.base.nameType)
        val name = fileReader.string(nameAndType.nameIndex)
        if (name == irFunction.name.asString()) {
            if (result == null)
                result = childFunction
            else {
                val resultIdSignature = symbolDeserializer.deserializeIdSignature(BinarySymbolData.decode(result.base.base.symbol).signatureId)
                if (resultIdSignature == signature)
                    return result
                result = childFunction
            }
        }
    }
    return result ?: error("Function ${irFunction.render()} is not found")
}
