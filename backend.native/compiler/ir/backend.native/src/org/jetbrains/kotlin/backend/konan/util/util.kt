package org.jetbrains.kotlin.backend.konan.util

import kotlin.system.measureTimeMillis

fun printMillisec(message: String, body: () -> Unit) {
    val msec = measureTimeMillis{
        body()
    }
    println("$message: $msec msec")
}

fun profile(message: String, body: () -> Unit) = profileIf(
    System.getProperty("konan.profile")?.equals("true") ?: false,
    message, body)

fun profileIf(condition: Boolean, message: String, body: () -> Unit) =
    if (condition) printMillisec(message, body) else body()

fun nTabs(amount: Int): String {
    return String.format("%1$-${(amount+1)*4}s", "") 
}

fun <T> Collection<T>.atMostOne(): T? {
    return when (this.size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

inline fun <T> Iterable<T>.atMostOne(predicate: (T) -> Boolean): T? = this.filter(predicate).atMostOne()