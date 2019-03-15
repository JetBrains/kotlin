// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.handler

import com.intellij.debugger.streams.wrapper.*
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl
import com.intellij.debugger.streams.wrapper.impl.TerminatorStreamCallImpl
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes

fun IntermediateStreamCall.withArgs(args: List<CallArgument>) =
    IntermediateStreamCallImpl(name, args, typeBefore, typeAfter, textRange)

fun TerminatorStreamCall.withArgs(args: List<CallArgument>) =
    TerminatorStreamCallImpl(name, args, typeBefore, resultType, textRange)

fun StreamCall.typeBefore() =
    if (StreamCall@ this is TypeBeforeAware) StreamCall@ this.typeBefore else KotlinSequenceTypes.ANY

fun StreamCall.typeAfter() =
    if (StreamCall@ this is TypeAfterAware) StreamCall@ this.typeAfter else KotlinSequenceTypes.ANY