// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun foo(x: Any, y: List<String>) { }
fun foo(x: Any, y: Set<Char>) { }

fun test(x: Any, y: Any, z: Any, t: Any, s: Any) {
    foo(x as String, [x])
    foo(y as Char, [y])

    foo([z as Char], [z])
    foo([if (true) t as Char else return], [t])
    foo(if (true) [s as Char] else return, [s])
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, lambdaLiteral, smartcast */
