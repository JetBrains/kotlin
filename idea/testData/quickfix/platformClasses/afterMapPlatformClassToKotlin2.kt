// "Change all usages of 'java.lang.Comparable<T>' in this file to 'jet.Comparable<T>'" "true"
import java.lang.*
import java.lang

fun <T> foo() : Comparable<T>? {
    return null
}

fun bar() : Comparable<String>? {
    return null
}

fun baz() : Comparable<String> {
    throw Exception()
}