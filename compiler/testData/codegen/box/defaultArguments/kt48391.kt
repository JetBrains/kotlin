// FILE: 1.kt

class B : A<String>

fun box(): String =
    B().f()!!

// FILE: 2.kt

interface A<T> {
    fun f(s: String = RESULT): T? =
        s as T

    companion object {
        private val RESULT = "OK"
    }
}
