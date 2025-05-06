// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        println(get(0))
    }
}
