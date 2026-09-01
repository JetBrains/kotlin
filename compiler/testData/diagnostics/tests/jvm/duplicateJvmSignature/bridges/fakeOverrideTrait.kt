// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface B<T> {
    fun foo(t: T) {}
}

class C : B<String> {
    <!ACCIDENTAL_OVERRIDE!>fun foo(o: Any)<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
