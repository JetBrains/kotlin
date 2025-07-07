// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-78866

interface Foo<T> {
    fun bar()
}

interface FooA : Foo<String>
interface FooB : Foo<Int>

fun test(a: FooA, b: FooB) {
    with(a) {
        context(b) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>bar<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
