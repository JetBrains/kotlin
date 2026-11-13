// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// FILE: declarations.kt

interface I

sealed class Parent {
    companion object {
        val bar: MyClass get() = MyClass()
        val barNullableReceiver: MyClass get() = MyClass()
        val barOtherType: MyClass get() = MyClass()
        val barDeprecated: MyClass get() = MyClass()
        val barFromObject: MyClass get() = MyClass()
        val barSupertypeOfCompanion: MyClass get() = MyClass()
        val barGeneric: MyClass get() = MyClass()
        val barHidden: MyClass get() = MyClass()
        val barWithContext: MyClass get() = MyClass()
        val barPrivate: MyClass get() = MyClass()
        val barInstanceExtension: MyClass get() = MyClass()
        val barFunction: MyClass get() = MyClass()
        val barTwoHidden: MyClass get() = MyClass()
        val barTwoPrivate: MyClass get() = MyClass()
        val barHiddenAndVisible: MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object : I
    }
}

fun foo(a: Parent.MyClass) {}

object O {
    val Any.barFromObject: String get() = ""
}

// FILE: privateHider.kt

private val Any.barPrivate: String get() = ""

// FILE: privateHider2.kt

private val Any.barTwoPrivate: String get() = ""

// FILE: privateHider3.kt

private val Any.barTwoPrivate: String get() = ""

// FILE: hidden1.kt
package hidden1

@Deprecated("", level = DeprecationLevel.HIDDEN) val Any.barTwoHidden: String get() = ""
val Any.barHiddenAndVisible: String get() = ""

// FILE: hidden2.kt
package hidden2

@Deprecated("", level = DeprecationLevel.HIDDEN) val Any.barTwoHidden: String get() = ""
@Deprecated("", level = DeprecationLevel.HIDDEN) val Any.barHiddenAndVisible: String get() = ""

// FILE: main.kt

import O.barFromObject
import hidden1.*
import hidden2.*

val Any.bar: String get() = ""
val Any?.barNullableReceiver: String get() = ""
val Any.barOtherType: String get() = ""
@Deprecated("") val Any.barDeprecated: String get() = ""
val I.barSupertypeOfCompanion: String get() = ""
val <T> T.barGeneric: String get() = ""
@Deprecated("", level = DeprecationLevel.HIDDEN) val Any.barHidden: String get() = ""
context(s: String) val Any.barWithContext: String get() = ""
val Parent.MyClass.barInstanceExtension: String get() = ""
fun Any.barFunction(): String = ""

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
    foo(<!UNRESOLVED_REFERENCE!>barNullableReceiver<!>)
    foo(<!UNRESOLVED_REFERENCE!>barOtherType<!>)
    foo(<!UNRESOLVED_REFERENCE!>barDeprecated<!>)
    foo(<!UNRESOLVED_REFERENCE!>barFromObject<!>)
    foo(<!UNRESOLVED_REFERENCE!>barSupertypeOfCompanion<!>)
    foo(<!UNRESOLVED_REFERENCE!>barGeneric<!>)
    foo(barHidden)
    foo(<!UNRESOLVED_REFERENCE!>barWithContext<!>)
    foo(barPrivate)
    foo(barInstanceExtension)
    foo(<!UNRESOLVED_REFERENCE!>barFunction<!>)
    foo(barTwoHidden)
    foo(barTwoPrivate)
    foo(<!UNRESOLVED_REFERENCE!>barHiddenAndVisible<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, getter,
interfaceDeclaration, nestedClass, nullableType, objectDeclaration, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, sealed, stringLiteral, typeParameter */
