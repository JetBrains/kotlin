// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
error class C {
    <!WRONG_MODIFIER_TARGET!>error<!> fun f() {}

    <!WRONG_MODIFIER_TARGET!>error<!> val readonly: Int
        <!WRONG_MODIFIER_TARGET!>error<!> get() = 1

    <!WRONG_MODIFIER_TARGET!>error<!> var mutable: Int
        <!WRONG_MODIFIER_TARGET!>error<!> get() = 1
        <!WRONG_MODIFIER_TARGET!>error<!> set(v) {}
}
error object O
<!WRONG_MODIFIER_TARGET!>error<!> interface I
<!WRONG_MODIFIER_TARGET!>error<!> annotation class A
<!WRONG_MODIFIER_TARGET!>error<!> enum class E

<!WRONG_MODIFIER_TARGET!>error<!> fun f() {}

<!WRONG_MODIFIER_TARGET!>error<!> val readonly: Int
    <!WRONG_MODIFIER_TARGET!>error<!> get() = 1

<!WRONG_MODIFIER_TARGET!>error<!> var mutable: Int
    <!WRONG_MODIFIER_TARGET!>error<!> get() = 1
    <!WRONG_MODIFIER_TARGET!>error<!> set(v) {}

val o = <!UNRESOLVED_REFERENCE!>error<!><!SYNTAX!><!> object<!SYNTAX!><!> {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, enumDeclaration, functionDeclaration, getter,
integerLiteral, interfaceDeclaration, objectDeclaration, propertyDeclaration */
