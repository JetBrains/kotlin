// RUN_PIPELINE_TILL: CODEGEN
// IDE_MODE
// ISSUE: KT-86093

// FILE: test.kt
package test

enum class A {
    X, Y, Z, W
}

// FILE: privates.kt
private val X: Int = 42
private object Y
class Z {
    private companion object
}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
object W

// FILE: main.kt
import test.A

fun expectsA(x: A) {}

fun main() {
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>)
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>)
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Z<!>)
    expectsA(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.W<!>)
    val a1: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>
    val a2: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>
    val a3: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Z<!>
    val a4: A = <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.W<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, localProperty, objectDeclaration, propertyDeclaration, stringLiteral */
