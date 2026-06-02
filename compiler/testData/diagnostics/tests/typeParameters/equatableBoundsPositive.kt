// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

fun <S, T> same(s: S, t: T) where S == T {}
fun <S, T, U> chain(s: S, t: T, u: U) where S == T, T == U {}
fun <S, T, U, V> twoPairs(s: S, t: T, u: U, v: V) where S == T, U == V {}
fun <S, T> sameWithBound(s: S, t: T) where S == T, S : Number {}

open class Base
class ChildA : Base()
class ChildB : Base()

fun testSameLiterals() {
    same(1, 2)
    same("hello", "world")
    same(true, false)
}

fun testSameVariable() {
    val x = 42
    same(x, x)
    val s = "text"
    same(s, s)
}

fun testExplicitSameTypeArgs() {
    same<Int, Int>(1, 2)
    same<String, String>("a", "b")
}

fun testExplicitCommonSupertype() {
    same<Number, Number>(1, 2.0)
}

fun testWithUpperBound() {
    sameWithBound(1, 2)
    sameWithBound(1L, 2L)
}

fun testChainAllSame() {
    chain(1, 2, 3)
    chain("x", "y", "z")
}

fun testTwoIndependentPairs() {
    twoPairs(1, 2, "a", "b")
    twoPairs(true, false, 1L, 2L)
}

fun testBothNullable() {
    val a: Int? = null
    val b: Int? = 1
    same(a, b)
}

fun testSameConcreteSubtype() {
    same(ChildA(), ChildA())
}

fun testInferredCommonSupertype() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>same<!>(ChildA(), ChildB())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
