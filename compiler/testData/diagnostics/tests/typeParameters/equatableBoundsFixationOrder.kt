// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T, U> chain3(s: S, t: T, u: U): S where S == T, T == U = s

fun positive() {
    chain3(1, 2, 3)
}

fun negativeFromLeft() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>chain3<!>("a", 2, 3)
}

fun negativeFromRight() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>chain3<!>(1, 2, "c")
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral, typeParameter */
