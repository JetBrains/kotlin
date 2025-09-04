// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
import kotlin.reflect.typeOf

inline fun <reified T : CharSequence> Int.foo(a: T) = typeOf<T>()

fun box(): String {
    if (42.foo("0123456789") != typeOf<String>())
        return "FAIL"
    return "OK"
}