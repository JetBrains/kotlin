/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.klib.UniqId
import org.jetbrains.kotlin.backend.common.serialization.cityHash64

internal class StubIrUniqIdProvider {
    private val mangler = KotlinLikeInteropMangler()

    fun uniqIdForFunction(function: FunctionStub): UniqId = with(mangler) {
        when (function.origin) {
            is StubOrigin.Function -> function.origin.function.uniqueSymbolName
            is StubOrigin.ObjCMethod -> {
                require(this.context is ManglingContext.Entity) {
                    "Unexpected mangling context $context for method ${function.name}."
                }
                function.origin.method.uniqueSymbolName
            }
            // TODO: What to do with "create" method in Objective-C categories?
            else -> error("Unexpected origin ${function.origin} for function ${function.name}.")
        }.toUniqId()
    }

    fun uniqIdForProperty(property: PropertyStub): UniqId = with (mangler) {
        when (property.origin) {
            is StubOrigin.ObjCProperty -> {
                require(this.context is ManglingContext.Entity) {
                    "Unexpected mangling context $context for property ${property.name}."
                }
                property.origin.property.uniqueSymbolName
            }
            is StubOrigin.Constant -> property.origin.constantDef.uniqueSymbolName
            is StubOrigin.Global -> property.origin.global.uniqueSymbolName
            // TODO: What to do with origin for enum entries and struct fields?
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

    private fun String.toUniqId() = UniqId(cityHash64())
}