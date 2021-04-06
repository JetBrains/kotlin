// !LANGUAGE: -DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: T!!): List<T!!>? = null
