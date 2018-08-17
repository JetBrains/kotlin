// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.collections

import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.debugger.sequence.psi.CallTypeExtractor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.KotlinPsiUtil
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes.ANY
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes.NULLABLE_ANY
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class KotlinCollectionsTypeExtractor : CallTypeExtractor.Base() {
    private companion object {
        val LOG = Logger.getInstance(KotlinCollectionsTypeExtractor::class.java)
    }

    override fun extractItemsType(type: KotlinType?): GenericType {
        if (type == null) return NULLABLE_ANY

        return tryToFindElementType(type) ?: defaultType(type)
    }

    override fun getResultType(type: KotlinType): GenericType {
        val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
        return KotlinSequenceTypes.primitiveTypeByName(typeName) ?: KotlinSequenceTypes.primitiveArrayByName(typeName) ?: getAny(type)
    }

    private fun tryToFindElementType(type: KotlinType): GenericType? {
        val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
        if (typeName == "kotlin.collections.Iterable" || typeName == "kotlin.Array") {
            if (type.arguments.isEmpty()) return NULLABLE_ANY
            val itemsType = type.arguments.first().type
            if (itemsType.isMarkedNullable) return NULLABLE_ANY
            val primitiveType = KotlinSequenceTypes.primitiveTypeByName(KotlinPsiUtil.getTypeWithoutTypeParameters(itemsType))
            return primitiveType ?: ANY
        }

        if (typeName == "kotlin.String" || typeName == "kotlin.CharSequence") return KotlinSequenceTypes.CHAR

        val primitiveArray = KotlinSequenceTypes.primitiveArrayByName(typeName)
        if (primitiveArray != null) return primitiveArray.elementType

        return type.supertypes().asSequence()
            .map(this::tryToFindElementType)
            .firstOrNull()
    }

    private fun defaultType(type: KotlinType): GenericType {
        LOG.warn("Could not find type of items for type ${KotlinPsiUtil.getTypeName(type)}")
        return getAny(type)
    }

    private fun getAny(type: KotlinType): GenericType = if (type.isMarkedNullable) NULLABLE_ANY else ANY
}