// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
<!WRONG_MODIFIER_TARGET!>error<!> class Foo
<!WRONG_MODIFIER_TARGET!>error<!> class Bar

fun foo(
    a: String | Foo,
    b: (String | Foo | Bar),
    c: String? | Foo | Bar,
    d: (String | Foo)?,
    e: Foo | Bar,
    f: (Foo | Bar)?,
    g: String | (Nothing | Foo),
    h: String | (Foo | Bar)?,
    i: Foo | Foo,
    j: Nothing? | Foo,
){
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType */
