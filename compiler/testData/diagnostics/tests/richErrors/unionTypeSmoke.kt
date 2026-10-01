// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
error class Foo
error class Bar

fun foo(
    a: String | Foo,
    b: (String | Foo | Bar),
    c: String? | Foo | Bar,
    d: (String | Foo)?,
    e: Foo | Bar,
    f: (Foo | Bar)?,
    g: String | (Nothing | Foo),
    h: <!NULLABLE_NESTED_UNION_TYPE!>String | (Foo | Bar)?<!>,
    i: Foo | Foo,
    j: Nothing? | Foo,
    k: <!NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE!>String | (String | Foo)<!>,
    l: <!NULLABLE_ERROR_COMPONENT_IN_UNION_TYPE!>String | Foo?<!>,
    m: <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>String | Int<!>,
    n: <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>String | <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>(Foo | Int)<!><!>,
    o: String | RichError,
){
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
