// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// DISABLE_WITH_PARSER: Psi

open class A
class B : A()
class C : A()
class Inv<T>

fun <S, T> Iterable<S>.contains2(element: T) where S == T {}
fun <S, T> assertEquals2(expected: S, actual: T) where S == T {}
fun <S, T, V> Map<out S, V>.containsKey2(key: T): Boolean where S == T = false

fun <T> emptyIterable(): Iterable<T> = TODO()

fun testContainsSameType(ints: Iterable<Int>, strs: Iterable<String>) {
    ints.contains2(4)
    strs.contains2("c")
}

fun testContainsDifferentType(ints: Iterable<Int>) {
    ints.<!EQUATABLE_TYPE_BOUND_VIOLATED!>contains2<!>("hello")
}

fun testContainsSubtype(as_: Iterable<A>) {
    as_.contains2(B())
}

fun testContainsUnboundNestedTypeParam(nested: Iterable<Iterable<Int>>) {
    nested.contains2(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Iterable<kotlin.Int>")!>emptyIterable()<!>)
}

fun testContainsInvariantDifferent(invNums: Iterable<Inv<Number>>) {
    invNums.<!EQUATABLE_TYPE_BOUND_VIOLATED!>contains2<!>(Inv<Int>())
}

fun testContainsInvariantSame(invInts: Iterable<Inv<Int>>) {
    invInts.contains2(Inv<Int>())
}

fun testAssertEqualsSameType() {
    assertEquals2(1, 2)
    assertEquals2("hello", "world")
}

fun testAssertEqualsSubtype(a: A, b: B) {
    assertEquals2(a, b)
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>assertEquals2<!>(b, a)
}

fun testAssertEqualsSiblingTypes(b: B, c: C) {
    <!EQUATABLE_TYPE_BOUND_VIOLATED!>assertEquals2<!>(b, c)
}

fun testMapContainsKeySameType(map: Map<String, Int>) {
    map.containsKey2("c")
}

fun testMapContainsKeyDifferentType(map: Map<String, Int>) {
    map.<!EQUATABLE_TYPE_BOUND_VIOLATED!>containsKey2<!>(42)
}

fun testMapContainsKeyUnbound(map: Map<out Any, Int>) {
    map.containsKey2("a")
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, nullableType,
outProjection, stringLiteral, typeParameter */
