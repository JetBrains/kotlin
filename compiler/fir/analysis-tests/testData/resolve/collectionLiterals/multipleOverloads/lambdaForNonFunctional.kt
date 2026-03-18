// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB


fun f1(x: IntArray) { }
fun <T> f1(x: List<T>) { }

fun <T: CharSequence> f2(x: Set<T>) { }
fun <T> f2(x: List<T>) { }

fun test() {
    f1([{ 42 }])
    f1([{}])
    f1([{ 42 }, { "42" }])

    f2([{ 42 }])
    f2([{ -> 42}])
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, lambdaLiteral, nullableType, stringLiteral,
typeParameter */
