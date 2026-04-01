// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84991

class Foo<T, U>

fun <T, U> foo(x: Foo<T & Any, U>) {}

class Bar<A, B> {
    fun bar(x: Foo<A, B>) {
        <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!ARGUMENT_TYPE_MISMATCH("Foo<A (of class Bar<A, B>), B (of class Bar<A, B>)>; Foo<uninferred T (of fun <T, U> foo) & Any, uninferred U (of fun <T, U> foo)>")!>x<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, functionDeclaration, nullableType, typeParameter */
