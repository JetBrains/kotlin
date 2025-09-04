// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
import kotlin.reflect.typeOf

class A {
    inner class Inner {
        inline fun <reified T : CharSequence> foo(a: T) = typeOf<T>()
    }
}

fun box(): String {
    if (A().Inner().foo("0123456789") != typeOf<String>())
        return "FAIL"
    return "OK"
}