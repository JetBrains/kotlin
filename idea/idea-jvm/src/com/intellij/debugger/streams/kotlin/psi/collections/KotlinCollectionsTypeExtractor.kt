// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.collections

import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor
import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor.IntermediateCallTypes
import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor.TerminatorCallTypes
import com.intellij.debugger.streams.kotlin.psi.KotlinPsiUtil
import com.intellij.debugger.streams.kotlin.psi.receiverType
import com.intellij.debugger.streams.kotlin.psi.resolveType
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes.ANY
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes.NULLABLE_ANY
import com.intellij.debugger.streams.trace.impl.handler.type.ArrayType
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionsTypeExtractor : CallTypeExtractor {
  private companion object {
    val LOG = Logger.getInstance(KotlinCollectionsTypeExtractor::class.java)
    val primitives: Set<GenericType> = setOf(KotlinTypes.BOOLEAN, KotlinTypes.BYTE, KotlinTypes.INT, KotlinTypes.SHORT,
        KotlinTypes.CHAR, KotlinTypes.LONG, KotlinTypes.FLOAT, KotlinTypes.DOUBLE)
    val primitiveArrays: Set<ArrayType> = primitives.map(KotlinTypes::array).toSet()
  }

  override fun extractIntermediateCallTypes(call: KtCallExpression): IntermediateCallTypes =
      IntermediateCallTypes(extractItemsType(call.receiverType()), extractItemsType(call.resolveType()))


  override fun extractTerminalCallTypes(call: KtCallExpression): TerminatorCallTypes =
      TerminatorCallTypes(extractItemsType(call.receiverType()), getResultType(call.resolveType()))

  private fun extractItemsType(type: KotlinType?): GenericType {
    if (type == null) return NULLABLE_ANY

    return tryToFindElementType(type) ?: defaultType(type)
  }

  private fun tryToFindElementType(type: KotlinType): GenericType? {
    val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
    if (typeName == "kotlin.collections.Iterable" || typeName == "kotlin.Array") {
      if (type.arguments.isEmpty()) return NULLABLE_ANY
      val itemsType = type.arguments.first().type
      if (itemsType.isMarkedNullable) return NULLABLE_ANY
      val primitiveType = tryToGetPrimitiveByName(KotlinPsiUtil.getTypeWithoutTypeParameters(itemsType))
      return primitiveType ?: ANY
    }

    if (typeName == "kotlin.String" || typeName == "kotlin.CharSequence") return KotlinTypes.CHAR

    val primitiveArray = tryToGetPrimitiveArrayByName(typeName)
    if (primitiveArray != null) return primitiveArray.elementType

    return type.supertypes().asSequence()
        .map(this::tryToFindElementType)
        .firstOrNull()
  }

  private fun defaultType(type: KotlinType): GenericType {
    LOG.warn("Could not find type of items for type ${KotlinPsiUtil.getTypeName(type)}")
    return getAny(type)
  }

  private fun getResultType(type: KotlinType): GenericType {
    val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
    return tryToGetPrimitiveByName(typeName) ?: tryToGetPrimitiveArrayByName(typeName) ?: getAny(type)
  }
  
  private fun getAny(type: KotlinType): GenericType = if (type.isMarkedNullable) NULLABLE_ANY else ANY

  private fun tryToGetPrimitiveByName(name: String): GenericType? =
      primitives.firstOrNull { x -> x.variableTypeName == name }

  private fun tryToGetPrimitiveArrayByName(name: String): ArrayType? =
      primitiveArrays.firstOrNull { it.variableTypeName == name }
}