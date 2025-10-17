// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
import kotlin.reflect.typeOf

inline fun <reified T : Number> foo(a: T) = typeOf<T>()

fun box(): String {
    val arguments = listOf<Number>(42, 4.20, -0, 1L, 0x0F, 0b00001011, 123.5e10, 123.5f, 1_000_000)
    for (arg in arguments)
        if (foo(arg) != typeOf<Number>())
            return arg.toString()
    return "OK"
}