/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.klib.UniqId
import org.jetbrains.kotlin.backend.common.serialization.cityHash64

/**
 * Returns stable [UniqId] for the given element of StubIr.
 */
internal class StubIrUniqIdProvider(private val context: ManglingContext) {
    private val mangler: InteropMangler = KotlinLikeInteropMangler(context)

    fun createChild(suffix: String): StubIrUniqIdProvider =
            StubIrUniqIdProvider(ManglingContext.Entity(suffix, context))

    fun uniqIdForFunction(function: FunctionStub): UniqId = with(mangler) {
        when (function.origin) {
            is StubOrigin.Function -> function.origin.function.uniqueSymbolName
            is StubOrigin.ObjCMethod -> function.origin.method.uniqueSymbolName
            is StubOrigin.ObjCCategoryInitMethod -> "${function.origin.method.uniqueSymbolName}#Create"
            is StubOrigin.Synthetic.EnumByValue -> "${function.origin.enum.uniqueSymbolName}#ByValue"
            else -> error("Unexpected origin ${function.origin} for function ${function.name}.")
        }.toUniqId()
    }

    fun uniqIdForProperty(property: PropertyStub): UniqId = with (mangler) {
        when (property.origin) {
            is StubOrigin.ObjCProperty -> property.origin.property.uniqueSymbolName
            is StubOrigin.Constant -> property.origin.constantDef.uniqueSymbolName
            is StubOrigin.Global -> property.origin.global.uniqueSymbolName
            // TODO: Is it correct for entries that are emitted as top-level constants?
            //  Should we emit the same uniq id for constants and enum entries?
            is StubOrigin.EnumEntry -> property.origin.constant.uniqSymbolName
            is StubOrigin.Synthetic.EnumValueField -> "${property.origin.enum.uniqueSymbolName}#Value"
            is StubOrigin.StructMember -> property.origin.member.name
            is StubOrigin.Synthetic.EnumVarValueField -> "${property.origin.enum.uniqueSymbolName}#Var"
            else -> error("Unexpected origin ${property.origin} for property ${property.name}.")
        }.toUniqId()
    }

    private fun InteropMangler.uniqSymbolNameForTypeAlias(origin: StubOrigin): String? = when (origin) {
        is StubOrigin.TypeDef -> origin.typedefDef.uniqueSymbolName
        is StubOrigin.Enum -> origin.enum.uniqueSymbolName
        is StubOrigin.VarOf -> uniqSymbolNameForTypeAlias(origin.typeOrigin)?.let { "$it#Var" }
        else -> null
    }

    fun uniqIdForTypeAlias(typeAlias: TypealiasStub): UniqId = with (mangler) {
        uniqSymbolNameForTypeAlias(typeAlias.origin)
                ?: error("Unexpected origin ${typeAlias.origin} for typealias ${typeAlias.alias.fqName}.")
    }.toUniqId()

    private fun InteropMangler.uniqSymbolNameForClass(origin: StubOrigin): String? = when (origin) {
        is StubOrigin.ObjCClass -> if (origin.isMeta) {
            origin.clazz.metaClassUniqueSymbolName
        } else {
            origin.clazz.uniqueSymbolName
        }
        is StubOrigin.ObjCProtocol -> if (origin.isMeta) {
            origin.protocol.metaClassUniqueSymbolName
        } else {
            origin.protocol.uniqueSymbolName
        }
        is StubOrigin.Struct -> origin.struct.uniqueSymbolName
        is StubOrigin.Enum -> origin.enum.uniqueSymbolName
        is StubOrigin.VarOf -> "${uniqSymbolNameForClass(origin.typeOrigin)}#Var"
        else -> null
    }

    fun uniqIdForClass(classStub: ClassStub): UniqId = with (mangler) {
        when (classStub) {
            is ClassStub.Simple,
            is ClassStub.Enum -> uniqSymbolNameForClass(classStub.origin)
            is ClassStub.Companion -> "${context.prefix}#Companion"
        } ?: error("Unexpected origin ${classStub.origin} for class ${classStub.classifier.fqName}.")
    }.toUniqId()

    private fun InteropMangler.uniqSymbolNameForConstructor(origin: StubOrigin): String? = when (origin) {
        is StubOrigin.Synthetic.DefaultConstructor -> "${context.prefix}#Constructor"
        is StubOrigin.Enum -> "${origin.enum.uniqueSymbolName}#Constructor"
        is StubOrigin.Struct -> "${origin.struct.uniqueSymbolName}#Constructor"
        is StubOrigin.ObjCMethod -> "${origin.method.uniqueSymbolName}#Constructor"
        else -> null
    }

    fun uniqIdForConstructor(constructorStub: ConstructorStub): UniqId = with (mangler) {
        uniqSymbolNameForConstructor(constructorStub.origin)
                ?: error("Unexpected origin ${constructorStub.origin} for constructor.")
    }.toUniqId()

    fun uniqIdForEnumEntry(enumEntry: EnumEntryStub, enum: ClassStub.Enum): UniqId = with (mangler) {
        "${uniqSymbolNameForClass(enum.origin)}#${enumEntry.origin.constant.name}"
    }.toUniqId()

    /**
     * MSB should be set to 1 for public declarations.
     * @see org.jetbrains.kotlin.ir.util.UniqId
     */
    private fun Long.markAsPublic() = this or (1L shl 63)

    private fun String.toUniqId() = UniqId(cityHash64().markAsPublic())
}