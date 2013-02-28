// "Change all usages of 'java.lang.Comparable<T>' in this file to 'jet.Comparable<T>'" "true"
import java.lang.*
import java.lang.Comparable
import java.lang.Comparable
import java.lang

fun <T> foo() : java.lang.Comparable<T>? {
    return null
}

fun bar() : lang.Comparable<String><caret>? {
    return null
}

fun baz() : Comparable<String> {
    throw Exception()
}