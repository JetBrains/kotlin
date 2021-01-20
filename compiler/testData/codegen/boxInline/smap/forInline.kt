// !LANGUAGE: +CorrectSourceMappingSyntax
// WITH_RUNTIME
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


// FILE: 1.smap

// FILE: 2.smap
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 2.kt
SomeIterator
*L
1#1,31:1
21#2,6:32
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1:32,6
*E