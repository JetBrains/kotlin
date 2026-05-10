// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND

fun <K> select(vararg k: K): K = k[0]
fun <I> id(i: I): I = i

fun cond(): Boolean = true

fun test() {
    select(id([42]), setOf<String>())
    select(id([]), setOf<String>())
    select(id([42]), setOf())
}

fun testWithRun() {
    val exp = when {
        cond() -> run { [42] }
        else -> setOf<String>()
    }
}

/* GENERATED_FIR_TAGS: capturedType, collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, outProjection, propertyDeclaration, typeParameter, vararg, whenExpression */
