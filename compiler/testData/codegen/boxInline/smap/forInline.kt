// !LANGUAGE: +CorrectSourceMappingSyntax
// WITH_STDLIB
// FILE: 1.kt
package test

inline fun stub() {

}

// FILE: 2.kt

fun box(): String {
    //Breakpoint!
    for (element in Some()) { // No inlining visible on this string
        return nonInline(element)
    }
    return "fail"
}

fun <T> nonInline(p: T): T = p

class Some() {
    operator fun iterator() = SomeIterator()
}

class SomeIterator {
    var result = "OK"

    inline operator fun hasNext() : Boolean {
        return result == "OK"
    }

    inline operator fun next(): String {
        result = "fail"
        return "OK"
    }
}
