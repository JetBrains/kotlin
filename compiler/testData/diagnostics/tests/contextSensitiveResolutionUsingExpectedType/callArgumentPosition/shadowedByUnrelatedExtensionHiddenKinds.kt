// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// FILE: parentCompanionExtension.kt
package parentCompanionExtension

sealed class Parent {
    companion object
    class MyClass : Parent() {
        companion object
    }
}

val Parent.Companion.bar: Parent.MyClass get() = Parent.MyClass()
val Any.bar: String get() = ""

fun foo(a: Parent.MyClass) {}

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
}

// FILE: twoSealedLevels.kt
package twoSealedLevels

sealed class G {
    companion object {
        val bar: G.P.MyClass get() = P.MyClass()
    }

    sealed class P : G() {
        companion object
        class MyClass : P()
    }
}

val Any.bar: String get() = ""

fun foo(a: G.P.MyClass) {}

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
}

// FILE: objectSubclass.kt
package objectSubclass

sealed class Parent {
    companion object {
        val bar: Parent.MyObj get() = MyObj
    }

    object MyObj : Parent()
}

val Any.bar: String get() = ""

fun foo(a: Parent.MyObj) {}

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
}

// FILE: e.kt
package e

enum class E {
    A, B;

    companion object
}

val E.Companion.first: E get() = E.A

// FILE: enumInWhen.kt
package enumInWhen

import e.*

val Any.first: String get() = ""

fun f(x: E) = when (x) {
    <!UNRESOLVED_REFERENCE!>first<!> -> 1
    else -> 2
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, getter, integerLiteral, nestedClass, objectDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, sealed, whenExpression, whenWithSubject */
