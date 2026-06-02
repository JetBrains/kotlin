// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T> same(s: S, t: T) where S == T {}

fun testNonNullableAndNullable() {
    val a: Int? = null
    val b: Int = 1
    same(a, b)
}

fun testNullableAndNonNullable() {
    val a: Int? = null
    val b: Int = 1
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>same<!>(b, a)
}

fun testBothNullable() {
    val a: Int? = null
    val b: Int? = null
    same(a, b)
}

fun testExplicitNullableTypeArgs() {
    same<Int?, Int?>(null, null)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter */
