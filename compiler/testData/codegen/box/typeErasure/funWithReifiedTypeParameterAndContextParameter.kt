// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// LANGUAGE: +ContextParameters
import kotlin.reflect.typeOf

context(c: Int)
inline fun <reified T : CharSequence> foo(a: T) = typeOf<T>()

fun box(): String = with(42) {
    if (foo("0123456789") != typeOf<String>())
        return "FAIL"
    return "OK"
}