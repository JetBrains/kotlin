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
    h: <!OTHER_ERROR!>String | (Foo | Bar)?<!>,
    i: Foo | Foo,
    j: Nothing? | Foo,
    k: <!OTHER_ERROR!>String | (String | Foo)<!>,
    l: <!OTHER_ERROR!>String | Foo?<!>,
    m: <!OTHER_ERROR!>String | Int<!>,
    n: <!OTHER_ERROR!>String | <!OTHER_ERROR!>(Foo | Int)<!><!>,
    o: String | RichError,
){
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
