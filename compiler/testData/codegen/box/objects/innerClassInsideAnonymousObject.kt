// ISSUE: KT-77021
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM, NATIVE

class Generic<T>(val t: T) {
    fun foo(): T {
        val o1 = object {
            inner class InnerLocal1 {
                fun bar(): T {
                    val o2 = object {
                        inner class InnerLocal2{
                            fun baz(): T = t
                        }
                    }
                    return o2.InnerLocal2().baz()
                }
            }
        }
        return o1.InnerLocal1().bar()
    }
}

fun box(): String {
    return Generic("OK").foo()
}

