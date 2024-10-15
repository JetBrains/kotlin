// RUN_PIPELINE_TILL: SOURCE

inline fun <T> tryLambdas(lamb : () -> T) : T{
    return lamb.invoke()
}
fun main() {
    tryLambdas<String> {
        <!ARGUMENT_TYPE_MISMATCH!>return@tryLambdas<!>
    }
}
