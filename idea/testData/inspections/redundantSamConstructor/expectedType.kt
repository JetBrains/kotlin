package redundantSamConstructor

import a.*
import a.A.*

fun testExpectedType() {
    val s1: JFunction1<String> = A.expectedType(JFunction1<String> { })
    val s2: JFunction1<String> = expectedType(JFunction1<String> { })
}