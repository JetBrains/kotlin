// FILE: inlineFunInMultiFilePackage.kt
package inlineFunInMultiFilePackage

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 1
}

// EXPRESSION: multiFilePackage.foo { 1 }
// RESULT: 1: I

// FILE: multiFilePackage.kt
@file:JvmMultifileClass
@file:JvmName("NewName")
package multiFilePackage

inline fun foo(f: () -> Int) = f()