// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-24202
interface A<T> {
    fun foo()
}


class B : <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>A<!> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, override,
typeParameter */
