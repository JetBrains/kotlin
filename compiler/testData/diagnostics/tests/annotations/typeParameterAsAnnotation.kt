// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class Foo<T> {
    @<!NOT_A_CLASS!>T<!>
    fun foo() = 0
}

class Bar<T : Annotation> {
    @<!NOT_A_CLASS!>T<!>
    fun foo() = 0
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, typeConstraint,
typeParameter */
