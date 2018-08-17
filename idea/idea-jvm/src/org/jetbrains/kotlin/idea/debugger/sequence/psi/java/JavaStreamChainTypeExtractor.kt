// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.java

import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.idea.debugger.sequence.psi.CallTypeExtractor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.KotlinPsiUtil
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny

class JavaStreamChainTypeExtractor : CallTypeExtractor.Base() {
    override fun extractItemsType(type: KotlinType?): GenericType {
        if (type == null) {
            return KotlinSequenceTypes.NULLABLE_ANY
        }

        return when (KotlinPsiUtil.getTypeWithoutTypeParameters(type)) {
            CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM -> KotlinSequenceTypes.INT
            CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM -> KotlinSequenceTypes.DOUBLE
            CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM -> KotlinSequenceTypes.LONG
            CommonClassNames.JAVA_UTIL_STREAM_BASE_STREAM -> KotlinSequenceTypes.NULLABLE_ANY
            else -> extractItemsType(type.getImmediateSuperclassNotAny())
        }
    }

    override fun getResultType(type: KotlinType): GenericType = ClassTypeImpl(KotlinPsiUtil.getTypeName(type))
}