// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
import kotlin.reflect.typeOf

inline fun <reified T> foo(a: T)
        where T : CharSequence,
              T : Comparable<T> =
    typeOf<T>()

fun box(): String {
    if (foo("0123456789") != typeOf<String>())
            return "FAIL"
    return "OK"
}