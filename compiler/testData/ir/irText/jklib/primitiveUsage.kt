// TARGET_BACKEND: JVM

fun testPrimitiveCalls() {
    val a = 1.plus(2)
    val b = true.not()
}

fun testPrimitiveReferences() {
    val plusRef: (Int, Int) -> Int = Int::plus
    val notRef: (Boolean) -> Boolean = Boolean::not
}
