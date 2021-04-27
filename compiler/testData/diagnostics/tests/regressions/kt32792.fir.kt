// !LANGUAGE: +NewInference

inline fun <T> tryLambdas(lamb : () -> T) : T{
    return lamb.invoke()
}
fun main() {
    <!INAPPLICABLE_CANDIDATE!>tryLambdas<!><String> {
        <!ARGUMENT_TYPE_MISMATCH!>return@tryLambdas<!>
    }
}
