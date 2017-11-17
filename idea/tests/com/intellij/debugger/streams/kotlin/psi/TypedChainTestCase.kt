package com.intellij.debugger.streams.kotlin.psi

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType

/**
 * @author Vitaliy.Bibaev
 */
abstract class TypedChainTestCase(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {

  protected fun doTest(producerAfterType: GenericType,
                       vararg intermediateAfterTypes: GenericType) {
    val elementAtCaret = configureAndGetElementAtCaret()
    assertNotNull(elementAtCaret)
    val chains = chainBuilder.build(elementAtCaret)
    assertFalse(chains.isEmpty())
    val chain = chains[0]
    val intermediateCalls = chain.intermediateCalls
    assertEquals(intermediateAfterTypes.size, intermediateCalls.size)
    assertEquals(producerAfterType, chain.qualifierExpression.typeAfter)

    if (intermediateAfterTypes.isNotEmpty()) {
      assertEquals(producerAfterType, intermediateCalls[0].typeBefore)
      for (i in 0 until intermediateAfterTypes.size - 1) {
        assertEquals(intermediateAfterTypes[i], intermediateCalls[i].typeAfter)
        assertEquals(intermediateAfterTypes[i], intermediateCalls[i + 1].typeBefore)
      }

      val lastAfterType = intermediateAfterTypes[intermediateAfterTypes.size - 1]
      assertEquals(lastAfterType, chain.terminationCall.typeBefore)
      val lastCall = intermediateCalls[intermediateCalls.size - 1]
      assertEquals(lastAfterType, lastCall.typeAfter)
    } else {
      assertEquals(producerAfterType, chain.terminationCall.typeBefore)
    }
  }

  override fun doTest() {
    fail("Use doTest(producerAfterType, vararg intermediateAfterTypes)")
  }
}