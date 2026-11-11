// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// FILE: a.kt
package a

val Parent.MyClass.Companion.barAmbiguous: Parent.MyClass get() = Parent.MyClass()
val Ambiguous.Companion.bar: Parent.MyClass get() = Parent.MyClass()

// FILE: b.kt
package b

val Parent.MyClass.Companion.barAmbiguous: Parent.MyClass get() = Parent.MyClass()
val Ambiguous.Companion.bar: Parent.MyClass get() = Parent.MyClass()

// FILE: main.kt
import a.*
import b.*

sealed class Parent {
    companion object {
        val bar: MyClass get() = MyClass()
        val barAmbiguous: MyClass get() = MyClass()
    }

    class MyClass : Parent(), Ambiguous {
        companion object
    }
}

interface Ambiguous {
    companion object
}

fun foo(a: Parent.MyClass) {}

fun <T> upperAmbiguous(t: T) where T : Parent, T : Ambiguous {}
fun <T : Ambiguous> lowerAndUpperAmbiguous(a: T, b: T) {}

fun main() {
    foo(Parent.MyClass.<!OVERLOAD_RESOLUTION_AMBIGUITY!>barAmbiguous<!>)
    foo(barAmbiguous)

    upperAmbiguous(bar)
    lowerAndUpperAmbiguous(Parent.MyClass(), bar)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, interfaceDeclaration, nestedClass,
objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, sealed, typeConstraint, typeParameter */
