fun <T> Any?.unsafeCast(): T = this as T

fun <R> foo(returnType: String): R {
    return when {
        returnType == "Nothing" -> throw Exception()
        else -> null.unsafeCast()
    }
}

fun box(): String {
    foo<String>("")
    return "OK"
}