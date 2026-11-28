// DUMP_IR_AFTER_INLINE
// FILE: 1.kt
class Outer<O>(val x: O) {
    inner class Inner<I>(val y: I) {
        inline fun <R> test(r: R) = object {
            fun run(): R {
                class LocalClass() {
                    fun foo(): R = r
                    fun bar(): I = y
                    fun baz(): O = x
                }
                return LocalClass().foo()
            }
        }

        fun testInner() {
            test("K")
        }
    }

    fun testOuter() {
        Inner("I").test("K")
    }
}

// FILE: 2.kt

fun box(): String {
    Outer("O").Inner("I").test("K")
    return "OK"
}
