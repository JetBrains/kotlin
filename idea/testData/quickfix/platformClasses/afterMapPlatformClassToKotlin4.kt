// "Change all usages of 'java.lang.Iterable<T>' in this file to a Kotlin class" "true"
import java.lang.*
import java.lang
import java.lang as jl

fun <T> a() : Iterable<T>? {
    return null
}

fun b() : Iterable<String>? {
    return null
}

fun c() : Iterable<String> {
    throw Exception()
}

fun d() : Iterable<String>? {
    return null
}

fun e() : Iterable<String>? {
    throw Exception()
}
