// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
error class Foo
error class Bar
error class Baz

fun test(
    a: (String | Foo) | Bar,
    b: (String? | Foo) | Bar,
    c: (String? | Foo)? | Bar,
    d: (String | Foo)? | Bar,
    e: (String? | Foo)? | Bar,
    f: (Foo | Bar)? | Baz,
    g: (Nothing? | Foo) | Baz,
    h: (Nothing? | Foo)? | Baz,
) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
