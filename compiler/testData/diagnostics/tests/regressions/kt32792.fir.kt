// !LANGUAGE: +NewInference

inline fun <T> tryLambdas(lamb : () -> T) : T{
    return lamb.invoke()
}
fun main() {
    tryLambdas<String> {
        return@tryLambdas
    }
}