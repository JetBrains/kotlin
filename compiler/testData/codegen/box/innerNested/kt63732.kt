// IGNORE_BACKEND_K1: ANY
// ^KT-63732
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

lateinit var foo: Any

class A<T : Any> {
    inner class B(x: T) {
        init {
            foo = x
        }
    }

    fun foo(t: T) {
        object {
            val something: B = B(t)
        }
    }
}

fun box(): String {
    A<String>().foo("OK")
    return foo as String
}