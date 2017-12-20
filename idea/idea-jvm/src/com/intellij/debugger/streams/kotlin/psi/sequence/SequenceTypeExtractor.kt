// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.sequence

import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor
import com.intellij.debugger.streams.kotlin.psi.KotlinPsiUtil
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * @author Vitaliy.Bibaev
 */
class SequenceTypeExtractor : CallTypeExtractor.Base() {
  private companion object {
    val LOG = Logger.getInstance(SequenceTypeExtractor::class.java)
  }

  override fun extractItemsType(type: KotlinType?): GenericType {
    if (type == null) return KotlinTypes.NULLABLE_ANY

    return tryToFindElementType(type) ?: defaultType(type)
  }

  override fun getResultType(type: KotlinType): GenericType {
    val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
    return KotlinTypes.primitiveTypeByName(typeName)
        ?: KotlinTypes.primitiveArrayByName(typeName)
        ?: ClassTypeImpl(KotlinPsiUtil.getTypeName(type))
  }

  private fun tryToFindElementType(type: KotlinType): GenericType? {
    val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
    if (typeName == "kotlin.sequences.Sequence") {
      if (type.arguments.isEmpty()) return KotlinTypes.NULLABLE_ANY
      val itemsType = type.arguments.single().type
      if (itemsType.isMarkedNullable) return KotlinTypes.NULLABLE_ANY
      val primitiveType = KotlinTypes.primitiveTypeByName(KotlinPsiUtil.getTypeWithoutTypeParameters(itemsType))
      return primitiveType ?: KotlinTypes.ANY
    }

    return type.supertypes().asSequence()
        .map(this::tryToFindElementType)
        .firstOrNull()
  }

  private fun defaultType(type: KotlinType): GenericType {
    LOG.warn("Could not find type of items for type ${KotlinPsiUtil.getTypeName(type)}")
    return if (type.isMarkedNullable) KotlinTypes.NULLABLE_ANY else KotlinTypes.ANY
  }
}