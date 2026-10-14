// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89779

class MyClass {
    companion object
}

val MyClass.Companion.bar: MyClass get() = MyClass()
val MyClass.Companion.baz: MyClass get() = MyClass()

fun foo(a: MyClass) {}

class A {
    val MyClass.Companion.bar: MyClass get() = MyClass()
    val MyClass.Companion.barbaz: MyClass get() = MyClass()

    fun main() {
        foo(<!NONE_APPLICABLE!>bar<!>)
        foo(baz)
        foo(barbaz)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver */
