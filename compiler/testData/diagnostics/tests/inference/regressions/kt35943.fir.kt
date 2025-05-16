// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION

class Inv<I>
fun <T> create(): Inv<T> = TODO()

fun main() {
    if (true) <!CANNOT_INFER_PARAMETER_TYPE!>create<!>() else null
}
