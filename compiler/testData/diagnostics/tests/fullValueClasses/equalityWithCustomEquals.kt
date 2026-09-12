// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-88520
// LANGUAGE: +FullValueClasses

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
    val a1 = <!EQUALITY_NOT_APPLICABLE!>x == y<!>
    val a2 = <!EQUALITY_NOT_APPLICABLE!>y == x<!>
    val a3 = <!EQUALITY_NOT_APPLICABLE!>x == i<!>
    val a4 = <!EQUALITY_NOT_APPLICABLE!>i == x<!>
    val a5 = <!EQUALITY_NOT_APPLICABLE!>x == u<!>
    val a6 = <!EQUALITY_NOT_APPLICABLE!>u == x<!>
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
