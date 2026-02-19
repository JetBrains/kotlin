// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class Base {
    companion object {
        annotation class Foo
    }
}

class Derived : Base() {

    @<!UNRESOLVED_REFERENCE!>Foo<!>
    fun foo() = 42
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, functionDeclaration, integerLiteral,
nestedClass, objectDeclaration */
