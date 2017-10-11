package codegen.propertyCallableReference.valModule

import kotlin.test.*

val x = 42

@Test fun runTest() {
    val p = ::x
    println(p.get())
}