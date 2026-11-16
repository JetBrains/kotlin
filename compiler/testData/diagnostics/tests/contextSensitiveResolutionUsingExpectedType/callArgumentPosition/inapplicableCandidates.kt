// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89779
// FILE: declarations.kt
package ppp

class C {
    companion object {
        val onlyInapplicable: C = C()
        val inapplicableAndInvisible: C = C()
        val invisibleInapplicable: C = C()
        val length: C = C()
    }
}

class Context<T>

context(_: Context<Int>)
val onlyInapplicable: String get() = ""

context(_: Context<Int>)
val inapplicableAndInvisible: String get() = ""

fun foo(c: C) {}

// FILE: privates.kt
package ppp

private val inapplicableAndInvisible: String = ""

context(_: Context<Int>)
private val invisibleInapplicable: String get() = ""

// FILE: main.kt
package ppp

fun test() {
    foo(onlyInapplicable)
    foo(inapplicableAndInvisible)
    foo(invisibleInapplicable)

    val a: C = onlyInapplicable
    val b: C = inapplicableAndInvisible
    val c: C = invisibleInapplicable
}

context(_: Context<String>)
fun testWrongContext() {
    foo(onlyInapplicable)
}

context(_: Context<Int>)
fun testMatchingContext() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>onlyInapplicable<!>)
}

fun String?.testUnsafeCall() {
    foo(length)
    val c: C = length
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, getter, localProperty, nullableType, objectDeclaration, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral, typeParameter */
