// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T, U> chain(s: S, t: T, u: U) where S == T, T == U {}

fun testChainAllSame() {
    chain(1, 2, 3)
}

fun testChainLastDiffers() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>chain<!>(1, 2, "hello")
}

fun testChainMiddleDiffers() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>chain<!>(1, "hello", "world")
}

fun testChainAllDiffer() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED, EQUATABLE_TYPE_BOUND_VIOLATED!>chain<!>(1, "hello", true)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral, typeParameter */
