// RUN_PIPELINE_TILL: FRONTEND
class Outer {
    inner class Inner {
        <!NESTED_CLASS_NOT_ALLOWED!>enum class TestNestedEnum<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, inner, nestedClass */
