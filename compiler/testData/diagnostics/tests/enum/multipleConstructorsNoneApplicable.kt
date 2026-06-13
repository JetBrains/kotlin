// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86940
enum class Foo(a: String) {
    <!NONE_APPLICABLE!>Bar<!>(),
    <!NONE_APPLICABLE!>Baz<!>,
    ;

    constructor(b: Int) : this(b.toString())
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, primaryConstructor, secondaryConstructor */
