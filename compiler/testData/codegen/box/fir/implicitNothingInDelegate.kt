// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// Note: this test passes in FIR but fails in FE 1.0 + IR
fun box(): String {
    val m1: Map<String, Any> = mapOf("foo" to "O")
    val m2: Map<String, *> = mapOf("baz" to "K")
    val foo: String by m1
    val baz: String by m2
    return foo + baz
}
