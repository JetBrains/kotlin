// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun cond(): Boolean = true
fun <K> select(vararg k: K): K = k[0]

fun test() {
    val a = when {
        cond() -> 42
        cond() -> intArrayOf()
        else -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    }

    select(42, intArrayOf(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
}

/* GENERATED_FIR_TAGS: capturedType, collectionLiteral, functionDeclaration, integerLiteral, localProperty, nullableType,
outProjection, propertyDeclaration, typeParameter, vararg, whenExpression */
