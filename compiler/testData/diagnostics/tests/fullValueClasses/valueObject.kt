// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_VARIABLE

value object Foo

value object Bar {
    val x: Int get() = 42
    fun member(): Int = x
}

class Outer {
    value object Nested
}

class WithCompanion {
    <!WRONG_MODIFIER_TARGET!>value<!> companion object
}

fun test(fn: Foo?) {
    val a1 = <!FORBIDDEN_IDENTITY_EQUALS!>Foo === Foo<!> || <!FORBIDDEN_IDENTITY_EQUALS!>Foo !== Foo<!>
    val a2 = <!FORBIDDEN_IDENTITY_EQUALS!>Foo === Bar<!> || <!FORBIDDEN_IDENTITY_EQUALS!>Foo !== Bar<!>

    val any = Any()
    val a3 = <!FORBIDDEN_IDENTITY_EQUALS!>any === Foo<!> || <!FORBIDDEN_IDENTITY_EQUALS!>Foo !== any<!>
    val a4 = <!FORBIDDEN_IDENTITY_EQUALS!>fn === Foo<!> || <!FORBIDDEN_IDENTITY_EQUALS!>fn !== Foo<!>

    val b1 = <!EQUALITY_NOT_APPLICABLE!>Foo == Bar<!>

    val b2 = Foo == Foo
    val b3 = fn == Foo
    val b4 = Bar.member()
    val b5 = Bar.x
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, disjunctionExpression, equalityExpression, functionDeclaration,
integerLiteral, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, smartcast, value */
