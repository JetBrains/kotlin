// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

value class A(val x: Int) {
    inner class B(val y: Int) {
        val parent = this@A
        val x get() = this@A.x
    }
}

fun box(): String {
    val a = A(10)
    val b = a.B(20)
    
    require(b.parent == a) { "Expected b.parent to be $a, but was ${b.parent}" }
    require(b.x == 10) { "Expected b.x to be 10, but was ${b.x}" }
    require(b.y == 20) { "Expected b.y to be 20, but was ${b.y}" }

    return "OK"
}
