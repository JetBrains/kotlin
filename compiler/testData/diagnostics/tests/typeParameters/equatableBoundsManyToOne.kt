// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T, U> twoSides(s: S, t: T, u: U) where S == T, S == U {}

fun testAllSame() {
    twoSides(1, 2, 3)
}

fun testSecondDiffers() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>twoSides<!>(1, "hello", 3)
}

fun testThirdDiffers() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>twoSides<!>(1, 2, "hello")
}

fun testBothDiffer() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED, EQUATABLE_TYPE_BOUND_VIOLATED!>twoSides<!>(1, "hello", true)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral, typeParameter */
