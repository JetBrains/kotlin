// RUN_PIPELINE_TILL: CODEGEN

sealed class Outer {
    class NestedSubClass : Outer() {
        fun foo() {
            Inner()
        }
    }

    private inner class Inner
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, nestedClass, sealed */
