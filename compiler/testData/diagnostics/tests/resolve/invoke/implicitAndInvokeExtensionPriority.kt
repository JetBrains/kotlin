// FIR_IDENTICAL
// ISSUE: KT-58943

class A {
    fun bar() {
        val foo: String.() -> Int = { 1 } // (1)
        fun String.foo(): String = "" // (2)
        with("2") {
            // In K1, foo variable + invokeExtension on implicit receiver is more prioritized than `foo() + implicit receiver`
            // So, for now, we're going to preserve that behavior in K2
            // For design, see KT-59528
            takeInt(foo())
        }
    }
}

fun takeInt(x: Int) {}