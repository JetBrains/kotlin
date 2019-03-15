// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler.OnEachCall

class KotlinCollectionsPeekCallFactory : PeekCallFactory {
    override fun createPeekCall(elementsType: GenericType, lambda: String): IntermediateStreamCall =
        OnEachCall(elementsType, lambda)
}