// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A {
    inner class B {
        <!NESTED_CLASS_NOT_ALLOWED!>enum class E<!> {
            ENTRY
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, inner, nestedClass */
