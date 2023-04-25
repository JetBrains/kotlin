// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

class A {
    val a: Number
        private field = 1

    val b: Number
        internal field = a + 3
}

fun box(): String {
    return if (A().b + 20 == 24) {
        "OK"
    } else {
        "fail: A().b = " + A().b.toString()
    }
}
