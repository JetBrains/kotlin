// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

fun <T> id(t: T): T = t

fun test() {
    id(id { [] })
    [{[]}]
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, nullableType, typeParameter */
