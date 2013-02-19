// "Change all usages of 'java.lang.Iterable<T>' in this file to a Kotlin class" "true"
import java.lang.Iterable
import java.lang.Iterable
import java.lang

fun <T> foo() : java.lang.Iterable<T><caret>? {
    return null
}

fun bar() : lang.Iterable<String>? {
    return null
}

fun baz() : Iterable<String>? {
    throw Exception()
}