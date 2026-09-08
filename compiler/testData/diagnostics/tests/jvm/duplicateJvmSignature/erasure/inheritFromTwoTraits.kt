// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>Baz()<!>: Foo<String>, Bar<Int> {
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, primaryConstructor,
typeParameter */
