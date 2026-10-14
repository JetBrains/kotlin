// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: b.kt
package b

fun foo(a: MyClass) {}

class MyClass {
    companion object

    companion {
        val bar: MyClass get() = MyClass()
        val baz: MyClass get() = MyClass()
    }
}

companion val MyClass.extBar: MyClass get() = MyClass()
companion val MyClass.extBaz: MyClass get() = MyClass()

// FILE: a.kt
import b.*

val Any.bar: MyClass get() = MyClass()
val Any.extBar: MyClass get() = MyClass()

fun main() {
    foo(bar)
    foo(baz)
    foo(extBar)
    foo(extBaz)
    foo(MyClass.bar)
    foo(MyClass.extBar)

    val x1: MyClass = bar
    val x2: MyClass = baz
    val x3: MyClass = extBar
    val x4: MyClass = extBaz
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, localProperty, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver */
