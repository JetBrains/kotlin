// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// LANGUAGE: +CompanionBlocks +CompanionExtensions

sealed class Parent {
    companion {
        val bar: MyClass get() = MyClass()
        val baz: MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object

        companion {
            val own: MyClass get() = MyClass()
        }
    }
}

companion val Parent.extBar: Parent.MyClass get() = Parent.MyClass()
companion val Parent.extBaz: Parent.MyClass get() = Parent.MyClass()
companion val Parent.MyClass.ownExt: Parent.MyClass get() = Parent.MyClass()

val Any.bar: Parent.MyClass get() = Parent.MyClass()
val Any.extBar: Parent.MyClass get() = Parent.MyClass()
val Any.own: Parent.MyClass get() = Parent.MyClass()
val Any.ownExt: Parent.MyClass get() = Parent.MyClass()

fun foo(a: Parent.MyClass) {}

fun main() {
    foo(<!UNRESOLVED_REFERENCE!>bar<!>)
    foo(baz)
    foo(<!UNRESOLVED_REFERENCE!>extBar<!>)
    foo(extBaz)
    foo(own)
    foo(ownExt)

    val x1: Parent.MyClass = <!UNRESOLVED_REFERENCE!>bar<!>
    val x2: Parent.MyClass = baz
    val x3: Parent.MyClass = <!UNRESOLVED_REFERENCE!>extBar<!>
    val x4: Parent.MyClass = extBaz
    val x5: Parent.MyClass = own
    val x6: Parent.MyClass = ownExt
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, localProperty, nestedClass,
objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, sealed */
