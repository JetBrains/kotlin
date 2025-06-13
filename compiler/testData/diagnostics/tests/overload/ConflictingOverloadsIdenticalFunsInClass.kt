// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor */
