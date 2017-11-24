package com.intellij.debugger.streams.kotlin.trace.impl.handler

import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.wrapper.*
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl
import com.intellij.debugger.streams.wrapper.impl.TerminatorStreamCallImpl

/**
 * @author Vitaliy.Bibaev
 */
fun IntermediateStreamCall.withArgs(args: List<CallArgument>) =
    IntermediateStreamCallImpl(name, args, typeBefore, typeAfter, textRange)

fun TerminatorStreamCall.withArgs(args: List<CallArgument>) =
    TerminatorStreamCallImpl(name, args, typeBefore, resultType, textRange)

fun StreamCall.typeBefore() =
    if (StreamCall@ this is TypeBeforeAware) StreamCall@ this.typeBefore else KotlinTypes.ANY

fun StreamCall.typeAfter() =
    if (StreamCall@ this is TypeAfterAware) StreamCall@ this.typeAfter else KotlinTypes.ANY