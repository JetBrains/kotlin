package com.intellij.debugger.streams.kotlin.psi.collection

import com.intellij.debugger.streams.kotlin.lib.KotlinCollectionSupportProvider
import com.intellij.debugger.streams.kotlin.psi.TypedChainTestCase
import com.intellij.debugger.streams.wrapper.StreamChainBuilder

/**
 * @author Vitaliy.Bibaev
 */
class TypedCollectionChainTest : TypedChainTestCase("collection/positive/types") {
  override val kotlinChainBuilder: StreamChainBuilder = KotlinCollectionSupportProvider().chainBuilder
}