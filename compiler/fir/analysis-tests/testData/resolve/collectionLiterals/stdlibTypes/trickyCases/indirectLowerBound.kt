// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun <K> select(vararg k: K): K = k[0]
fun <I> id(i: I): I = i

fun cond(): Boolean = true

fun test() {
    select(id(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>), setOf<String>())
    select(id([]), setOf<String>())
    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>), <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>())
}

fun testWithRun() {
    val exp = when {
        cond() -> run { <!ARGUMENT_TYPE_MISMATCH!>[42]<!> }
        else -> setOf<String>()
    }
}

/* GENERATED_FIR_TAGS: capturedType, collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, outProjection, propertyDeclaration, typeParameter, vararg, whenExpression */
