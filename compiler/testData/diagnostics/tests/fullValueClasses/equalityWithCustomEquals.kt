// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88520
// LANGUAGE: +FullValueClasses
// DIAGNOSTICS: -UNUSED_VARIABLE

value class WithCustomEquals(val a: Int) {
    override fun equals(other: Any?): Boolean =
        other is WithCustomEquals && other.a == a || other is OtherWithCustomEquals && other.b == a
}

value class OtherWithCustomEquals(val b: Int) {
    override fun equals(other: Any?): Boolean =
        other is WithCustomEquals && other.a == b || other is OtherWithCustomEquals && other.b == b
}

value class Plain(val c: Int)
value class OtherPlain(val d: Int)

class Identityful

interface Unrelated

fun withCustomEquals(x: WithCustomEquals, y: OtherWithCustomEquals, i: Identityful, u: Unrelated) {
    val a1 = x == y
    val a2 = y == x
    val a3 = x == i
    val a4 = i == x
    val a5 = x == u
    val a6 = u == x
}

fun withGeneratedEquals(x: Plain, y: OtherPlain, i: Identityful, u: Unrelated) {
    val b1 = <!EQUALITY_NOT_APPLICABLE!>x == y<!>
    val b2 = <!EQUALITY_NOT_APPLICABLE!>x == i<!>
    val b3 = <!EQUALITY_NOT_APPLICABLE!>x == u<!>
}

fun identity(x: WithCustomEquals, y: OtherWithCustomEquals) {
    val c1 = <!FORBIDDEN_IDENTITY_EQUALS!>x === y<!>
    val c2 = <!FORBIDDEN_IDENTITY_EQUALS!>x !== y<!>
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, disjunctionExpression, equalityExpression, functionDeclaration,
interfaceDeclaration, isExpression, localProperty, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, value */
