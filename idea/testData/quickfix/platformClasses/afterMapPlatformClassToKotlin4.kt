// "Change all usages of 'java.lang.Iterable<T>' in this file to a Kotlin class" "true"
import java.lang

fun <T> foo() : Iterable<T>? {
    return null
}

fun bar() : Iterable<String>? {
    return null
}

fun baz() : Iterable<String>? {
    throw Exception()
}