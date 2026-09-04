// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

class Foo<A, B> {
    @JvmName("barA")
    fun bar(a: A) = Unit

    @JvmName("barB")
    fun bar(b: B) = Unit
}

@JvmName("bazA")
fun <A, B> Foo<A, B>.baz(a: A) = Unit

@JvmName("bazB")
fun <A, B> Foo<A, B>.baz(b: B) = Unit

fun test() {
    Foo<Array<String>, CharSequence>().bar([])
    Foo<Array<String>, CharSequence>().<!OVERLOAD_RESOLUTION_AMBIGUITY!>baz<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, stringLiteral, typeParameter */
