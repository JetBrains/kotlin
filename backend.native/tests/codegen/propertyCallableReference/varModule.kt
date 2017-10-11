package codegen.propertyCallableReference.varModule

import kotlin.test.*

var x = 42

@Test fun runTest() {
    val p = ::x
    p.set(117)
    println(x)
    println(p.get())
}