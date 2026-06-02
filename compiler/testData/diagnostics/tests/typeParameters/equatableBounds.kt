// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T> same(s: S, t: T): S where S == T = s

fun test() {
    same(1, 2)
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>same<!>(1, "hello")
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral, typeParameter */
