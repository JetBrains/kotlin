// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

class Baz(): Foo<String>, Bar<Int> {
    <!ACCIDENTAL_OVERRIDE!>fun foo(l: List<Long>)<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, primaryConstructor,
typeParameter */
