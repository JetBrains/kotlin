// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals

fun <T> id(t: T): T = t

fun test() {
    val a: () -> Set<Int> = id(id { [] })
    id(id { [1, 2, 3] })
    [{[1, 2, 3]}]

    val b: () -> (() -> Set<Int>) = { { [] } }
    { { [1, 2, 3] } }
    val c: () -> (() -> Set<Int>) = id { { [] } }
    id { { [1, 2, 3] } }
    val d: () -> (() -> Set<*>) = { { [1, 2, 3] } }

    val e = id(id { lbl@{ if(true) return@lbl []; [1, 2, 3] }})
    val f: () -> (() -> Set<*>) = id(id { lbl@{ if(true) return@lbl []; [1, 2, 3] }})

    val g = [{[[[{{{[[{[{}]}]]}}}]]]}]
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, starProjection, typeParameter */
