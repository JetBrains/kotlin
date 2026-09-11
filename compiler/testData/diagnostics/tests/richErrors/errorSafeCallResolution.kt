// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
error class Foo
error class Bar

abstract class C {
    abstract fun memberFun(): String | Bar
    abstract val memberVal: String | Bar
}

fun test(
    a: String | Foo,
    b: C | Foo,
    c: Foo | Bar,
) {
    val x1 = a<!UNNECESSARY_SAFE_CALL!>|.<!>length
    val x2 = b<!UNNECESSARY_SAFE_CALL!>|.<!>memberFun()
    val x3 = b<!UNNECESSARY_SAFE_CALL!>|.<!>memberVal
    val x4 = c<!UNNECESSARY_SAFE_CALL!>|.<!>toString()
    val x5 = ""<!UNNECESSARY_SAFE_CALL!>|.<!>length
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall */
