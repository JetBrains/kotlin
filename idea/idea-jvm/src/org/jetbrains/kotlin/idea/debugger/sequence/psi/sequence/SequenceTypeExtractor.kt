// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence

import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.debugger.sequence.psi.CallTypeExtractor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.KotlinPsiUtil
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

class SequenceTypeExtractor : CallTypeExtractor.Base() {
    private companion object {
        val LOG = Logger.getInstance(SequenceTypeExtractor::class.java)
    }

    override fun extractItemsType(type: KotlinType?): GenericType {
        if (type == null) return KotlinSequenceTypes.NULLABLE_ANY

        return tryToFindElementType(type) ?: defaultType(type)
    }

    override fun getResultType(type: KotlinType): GenericType {
        val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
        return KotlinSequenceTypes.primitiveTypeByName(typeName)
                ?: KotlinSequenceTypes.primitiveArrayByName(typeName)
                ?: ClassTypeImpl(KotlinPsiUtil.getTypeName(type))
    }

    private fun tryToFindElementType(type: KotlinType): GenericType? {
        val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
        if (typeName == "kotlin.sequences.Sequence") {
            if (type.arguments.isEmpty()) return KotlinSequenceTypes.NULLABLE_ANY
            val itemsType = type.arguments.single().type
            if (itemsType.isMarkedNullable) return KotlinSequenceTypes.NULLABLE_ANY
            val primitiveType = KotlinSequenceTypes.primitiveTypeByName(KotlinPsiUtil.getTypeWithoutTypeParameters(itemsType))
            return primitiveType ?: KotlinSequenceTypes.ANY
        }

        return type.supertypes().asSequence()
            .map(this::tryToFindElementType)
            .firstOrNull()
    }

    private fun defaultType(type: KotlinType): GenericType {
        LOG.warn("Could not find type of items for type ${KotlinPsiUtil.getTypeName(type)}")
        return if (type.isMarkedNullable) KotlinSequenceTypes.NULLABLE_ANY else KotlinSequenceTypes.ANY
    }
}