// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
sealed class Base {
    fun foo() = <!SEALED_CLASS_CONSTRUCTOR_CALL!>Base()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, sealed */
