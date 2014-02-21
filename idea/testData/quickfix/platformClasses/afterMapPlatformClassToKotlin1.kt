// "Change all usages of 'java.lang.Comparable<T>' in this file to 'kotlin.Comparable<T>'" "true"
import java.lang.*
import java.lang
import java.lang as jl

fun <T> a() : Comparable<T>? {
    return null
}

fun b() : Comparable<String> {
    throw Exception()
}

fun c() : Comparable<String> {
    throw Exception()
}

fun d() : Comparable<String>? {
    return null
}

fun e() : Comparable<String>? {
    throw Exception()
}
