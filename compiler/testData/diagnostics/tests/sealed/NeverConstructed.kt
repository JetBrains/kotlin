// RUN_PIPELINE_TILL: FRONTEND
sealed class Base {
    fun foo() = <!SEALED_CLASS_CONSTRUCTOR_CALL!>Base()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, sealed */
