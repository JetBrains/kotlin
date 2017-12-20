// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.sequence

import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author Vitaliy.Bibaev
 */
class SequenceTypeExtractor : CallTypeExtractor.Base() {
  override fun extractItemsType(type: KotlinType?): GenericType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getResultType(type: KotlinType): GenericType {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}