// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78985

fun <T> foo() {
    class C {
        class D {
            fun c(): C = C()
        }
    }
}

fun <U> bar() {
    class A {
        companion object {
            fun a(): A = A()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, nestedClass, nullableType, stringLiteral,
typeParameter */
