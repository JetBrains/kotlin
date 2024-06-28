// DIAGNOSTICS: -UNUSED_EXPRESSION

class Inv<I>
fun <T> create(): Inv<T> = TODO()

fun main() {
    <!NEW_INFERENCE_ERROR!>if (true) <!CANNOT_INFER_PARAMETER_TYPE!>create<!>() else null<!>
}
