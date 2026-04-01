// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3.0
// ^^^ K/Wasm backend v.2.3.0 did not raise NothingValueException. Fixed only in 2.3.20-Beta1, commit 2074edc.
//     So, a test `current frontend + 2.3.0 backend` expectedly fails
fun <T> something(): T = Any() as T

class Context<T>

fun <T> Any.decodeIn(typeFrom: Context<in T>): T = something()

fun <T> Any?.decodeOut(typeFrom: Context<out T>): T {
    return this?.decodeIn(typeFrom) // decodeIn result is of type Nothing
        ?: throw AssertionError("")
}

fun box(): String {
    try {
        "str".decodeOut(Context<Any>())
    } catch (e: Exception) {
        // TODO check FQN
        val exceptionClassName = e::class.simpleName
        if (exceptionClassName != "KotlinNothingValueException") {
            throw AssertionError("Unexpected exception: $e")
        } else {
            return "OK"
        }
    }

    throw AssertionError("Should fail with exception")
}