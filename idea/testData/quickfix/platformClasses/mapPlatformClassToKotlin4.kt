// "Change all usages of 'java.lang.Iterable<T>' in this file to a Kotlin class" "true"
import java.lang.*
import java.lang.Iterable
import java.lang.Iterable
import java.lang.Iterable as Foo
import java.lang
import java.lang as jl

fun <T> a() : java.lang.Iterable<T>? {
    return null
}

fun b() : lang.Iterable<String>? {
    return null
}

fun c() : Foo<String><caret> {
    throw Exception()
}

fun d() : jl.Iterable<String>? {
    return null
}

fun e() : Iterable<String>? {
    throw Exception()
}
