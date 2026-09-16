// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
<!WRONG_MODIFIER_TARGET!>error<!> class E1
<!WRONG_MODIFIER_TARGET!>error<!> class E2 : RichError()

<!WRONG_MODIFIER_TARGET!>error<!> object EO1
<!WRONG_MODIFIER_TARGET!>error<!> object EO2 : RichError()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration */
