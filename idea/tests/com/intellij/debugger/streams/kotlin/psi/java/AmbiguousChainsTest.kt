package com.intellij.debugger.streams.kotlin.psi.java

/**
 * @author Vitaliy.Bibaev
 */
class AmbiguousChainsTest : PositiveJavaStreamTest("ambiguous") {
  fun testSimpleExpression() = doTest(2)

  fun testNestedExpression() = doTest(3)
  fun testSimpleFunctionParameter() = doTest(2)
  fun testNestedFunctionParameters() = doTest(3)
  fun testNestedFunctionParametersReversed() = doTest(3)

  fun testStreamProducerParameter() = doTest(2)
  fun testStreamIntermediateCallParameter() = doTest(2)
  fun testStreamTerminatorParameter() = doTest(2)
  fun testStreamAllPositions() = doTest(4)

  fun testNestedStreamProducerParameter() = doTest(3)
  fun testNestedStreamIntermediateCallParameter() = doTest(3)
  fun testNestedStreamTerminatorCallParameter() = doTest(3)

  fun testNestedCallInLambda() = doTest(2)
  fun testNestedCallInAnonymous() = doTest(2)

  fun testLinkedChain() = doTest(3)

  private fun doTest(chainsCount: Int) {
    val chains = buildChains()
    assertEquals(chainsCount, chains.size)
  }
}