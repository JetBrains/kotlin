// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// WITH_STDLIB

sealed class Parent {
    companion object {
        val String.barMemberExtension: String get() = ""
    }

    class MyClass : Parent()
}

fun foo(a: Parent) {}

fun test() {
    foo(<!UNRESOLVED_REFERENCE!>barMemberExtension<!>)
    with("") {
        foo(<!UNRESOLVED_REFERENCE!>barMemberExtension<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, lambdaLiteral, nestedClass,
objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, sealed, stringLiteral */
