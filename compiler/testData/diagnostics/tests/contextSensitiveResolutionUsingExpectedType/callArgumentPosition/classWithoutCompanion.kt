// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80943

// FILE: foo.kt
package foo

enum class Enum {
    A, B, C
}

class A
interface B

// FILE: main.kt
package bar

import foo.Enum
import foo.A
import foo.B

class C

fun takesEnum(e: Enum) {}
fun takesInt(i: Int) {}

fun test() {
    takesEnum(A)
    takesEnum(B)
    takesEnum(C)
}

fun negative() {
    takesInt(<!ARGUMENT_TYPE_MISMATCH, NO_COMPANION_OBJECT!>A<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, interfaceDeclaration */
