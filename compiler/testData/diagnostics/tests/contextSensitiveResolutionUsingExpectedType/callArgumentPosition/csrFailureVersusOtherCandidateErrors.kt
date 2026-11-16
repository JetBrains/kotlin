// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89779
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// FIR_DUMP
// FILE: other.kt
package ppp

private fun invisibleOrCsrFailure(u: Unrelated): Unrelated = u

// FILE: main.kt
package ppp

enum class Color { Red }
class Unrelated

fun invisibleOrCsrFailure(c: Color): Color = c

class A {
    private fun member(u: Unrelated): Unrelated = u
    fun member(c: Color): Color = c
}

fun String.unsafeOrCsrFailure(u: Unrelated): Unrelated = u
fun String?.unsafeOrCsrFailure(c: Color): Color = c

fun test(a: A, s: String?) {
    val x = invisibleOrCsrFailure(<!UNRESOLVED_REFERENCE!>Blue<!>)
    val y = a.member(<!UNRESOLVED_REFERENCE!>Blue<!>)
    val z = s.unsafeOrCsrFailure(<!UNRESOLVED_REFERENCE!>Blue<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration,
localProperty, nullableType, propertyDeclaration */
