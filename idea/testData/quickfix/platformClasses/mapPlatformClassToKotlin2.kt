// "Change all usages of 'java.lang.Comparable<T>' in this file to 'kotlin.Comparable<T>'" "true"
import java.lang.*
import java.lang.Comparable
import java.lang.Comparable
import java.lang.Comparable as Foo
import java.lang
import java.lang as jl

fun <T> a() : java.lang.Comparable<T>? {
    return null
}

fun b() : lang.Comparable<String> {
    throw Exception()
}

fun c() : Foo<String> {
    throw Exception()
}

fun d() : jl.Comparable<String><caret>? {
    return null
}

fun e() : Comparable<String>? {
    throw Exception()
}
