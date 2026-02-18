// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB

// FILE: lib.kt
import kotlin.reflect.typeOf

class A {
    inline fun <reified T : CharSequence> foo(a: T) = typeOf<T>()
}

// FILE: main.kt
import kotlin.reflect.typeOf

fun box(): String {
    if (A().foo("0123456789") != typeOf<String>())
        return "FAIL"
    return "OK"
}