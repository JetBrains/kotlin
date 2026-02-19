// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB

// FILE: lib.kt
import kotlin.reflect.typeOf

class A {
    class Nested {
        inline fun <reified T : CharSequence> foo(a: T) = typeOf<T>()
    }
}

// FILE: main.kt
import kotlin.reflect.typeOf

fun box(): String {
    if (A.Nested().foo("0123456789") != typeOf<String>())
            return "FAIL"
    return "OK"
}