// WITH_STDLIB
// IGNORE_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

var result = ""

fun ex1_(res: Result<Int>) {
    res.fold(
        onSuccess = { result += "Ex $it\n" },
        onFailure = {},
    )
}

fun ex1(res: Result<Int>) {
    res.fold(
        onSuccess = { ex1_(res) },
        onFailure = { ex1_(Result.failure(it)) }
    )
}

val ex2_: (Result<Int>) -> Unit = { res ->
    res.fold(
        onSuccess = { result += "Ex $it\n" },
        onFailure = {},
    )
}

val ex2: (Result<Int>) -> Unit = { res ->
    res.fold(
        onSuccess = { ex2_(Result.success(it)) },
        onFailure = { ex2_(Result.failure(it)) }
    )
}


val ex3_: (Result<Int>) -> Unit = { res ->
    res.fold(
        onSuccess = { result += "Ex $it\n" },
        onFailure = {},
    )
}

val ex3: (Result<Int>) -> Unit = { res ->
    res.fold(
        onSuccess = { ex3_(res) },
        onFailure = { ex3_(Result.failure(it)) }
    )
}

fun box(): String {
    ex1(Result.success(1))
    ex2(Result.success(2))
    ex3(Result.success(3))
    return if (result == "Ex 1\nEx 2\nEx 3\n") "OK" else "FAIL $result"
}
