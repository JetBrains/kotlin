package com.intellij.debugger.streams.kotlin.resolve

import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.TraceInfo

/**
 * @author Vitaliy.Bibaev
 */
class FilterOrderResolver : ValuesOrderResolver {
  override fun resolve(info: TraceInfo): ValuesOrderResolver.Result {
    return ValuesOrderResolver.Result.of(emptyMap(), emptyMap())
  }
}