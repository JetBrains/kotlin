// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// LANGUAGE: +ContextParameters

// FILE: lib.kt
import kotlin.reflect.typeOf

class A {
    context(c: Int)
    inline fun <reified T : CharSequence> A.foo(a: T) = typeOf<T>()
}

// FILE: main.kt
import kotlin.reflect.typeOf

fun box(): String = with(42) {
    if (A().run { foo("0123456789") } != typeOf<String>())
            return "FAIL"
    return "OK"
}