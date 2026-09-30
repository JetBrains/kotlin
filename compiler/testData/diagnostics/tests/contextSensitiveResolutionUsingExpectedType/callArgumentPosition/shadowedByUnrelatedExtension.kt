// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// FILE: b.kt
package b

fun foo(a: MyClass) {}

class MyClass {
    companion object
}

val MyClass.Companion.bar: MyClass get() = MyClass()
val MyClass.Companion.baz: MyClass get() = MyClass()

// FILE: a.kt
import b.*

val Any.bar: MyClass get() = MyClass()

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
    foo(baz)
    foo(MyClass.bar)

    val x: MyClass = <!UNRESOLVED_REFERENCE!>bar<!>
    val y: MyClass = baz
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, localProperty, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver */
