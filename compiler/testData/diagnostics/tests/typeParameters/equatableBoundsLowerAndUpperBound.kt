// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T> transform(consumer: (T) -> Unit, value: S): T where S == T = null!!

open class Base
class DerivedA : Base()
class DerivedB : Base()

fun testSimpleBounds() {
    val result = transform({ x: Int -> }, 42)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

fun testBounds() {
    val result = transform({ x: DerivedA -> }, Base())
    <!DEBUG_INFO_EXPRESSION_TYPE("DerivedA")!>result<!>
}

fun testSimpleConflictingBounds() {
    val result = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>transform<!>({ x: Int -> }, "test")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.String")!>result<!>
}

fun testConflictingBounds() {
    // May be updated once equality bound is computed properly.
    val result = <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>transform<!>({ x: DerivedB -> }, DerivedA())
    <!DEBUG_INFO_EXPRESSION_TYPE("DerivedB & DerivedA")!>result<!>
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, functionalType, integerLiteral,
intersectionType, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
