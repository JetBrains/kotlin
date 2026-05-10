// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun <T> foo(s: Set<T>, x: Int) { }
fun <T> foo(s: Sequence<T>, x: String) { }

fun <T> bar(t: T, x: Int) { }
fun <T> bar(t: T, x: String) { }

fun <T> baz(x: Int, t: T) { }
fun <T> baz(x: String, t: T) { }

fun test(x: Any, y: Any, z: Any) {
    foo([x as String], x)
    bar([y as String], y)
    <!NONE_APPLICABLE!>baz<!>(z, [z as String])
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, nullableType, smartcast, typeParameter */
