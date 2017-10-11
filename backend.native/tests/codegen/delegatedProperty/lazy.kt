package codegen.delegatedProperty.lazy

import kotlin.test.*

val lazyValue: String by lazy {
    println("computed!")
    "Hello"
}

@Test fun runTest() {
    println(lazyValue)
    println(lazyValue)
}