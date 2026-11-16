// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89822, KT-89779
// FILE: a.kt
package a

val String.barWrongReceivers: String get() = ""
val String.barWrongReceiversExplicitImport: String get() = ""
val String.barWrongReceiverAndHidden: String get() = ""
val Any.barApplicableHiders: String get() = ""

// FILE: b.kt
package b

val String.barWrongReceivers: String get() = ""
val String.barWrongReceiversExplicitImport: String get() = ""
@Deprecated("", level = DeprecationLevel.HIDDEN) val Any.barWrongReceiverAndHidden: String get() = ""
val Any.barApplicableHiders: String get() = ""

// FILE: main.kt
import a.*
import b.*
import a.barWrongReceiversExplicitImport
import b.barWrongReceiversExplicitImport

sealed class Parent {
    companion object {
        val barWrongReceivers: MyClass get() = MyClass()
        val barWrongReceiversExplicitImport: MyClass get() = MyClass()
        val barWrongReceiverAndHidden: MyClass get() = MyClass()
        val barApplicableHiders: MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object
    }
}

fun foo(a: Parent.MyClass) {}

fun main() {
    foo(barWrongReceivers)
    foo(barWrongReceiversExplicitImport)
    foo(barWrongReceiverAndHidden)
    foo(barApplicableHiders)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, nestedClass, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver, sealed, stringLiteral */
