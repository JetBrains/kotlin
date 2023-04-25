// TARGET_FRONTEND: FIR
// FIR_IDENTICAL

// IGNORE_BACKEND_KLIB: JS_IR

class A {
    val a: Number
        private field = 1

    val b: Number
        internal field = a + 2

    val c = 1
    val d = c + 2

    fun rest() {
        val aI = A().a + 10
        val bI = A().b + 20
    }
}

fun test() {
    val bA = A().b + 20
}
