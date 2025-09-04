// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
import kotlin.reflect.typeOf

inline fun <reified T> foo(a: T) = typeOf<T>()

fun box(): String {
    val arguments = listOf<Any?>("0123456789", 4.20, true, null)
    for (arg in arguments)
        if (foo(arg) != typeOf<Any?>())
            return arg.toString()
    return "OK"
}
