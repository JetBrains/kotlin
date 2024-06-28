// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS -UNUSED_VARIABLE
// FILE: Test.java

class Test {
    static Number[] flexibleNumbers() {
        return null;
    }
}

// FILE: main.kt

fun <T> foo(x: Array<out T>): T = x[0]

inline fun <reified T> materializeArray(): Array<T> = null as Array<T>

fun main()  {
    val y = foo(Test.flexibleNumbers() ?: materializeArray()) // Any? in NI, Number! in OI (T of `materializeArray` is inferred to Any?)
}
