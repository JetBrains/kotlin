// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses

// MODULE: m1
// FILE: f1.kt
package p

value object A0 {
    val p: String get() = "!"
}

value object A1 {
    val p: String get() = "!"
    override fun equals(other: Any?) = other is A1
}

abstract value class AC {
    open val q: String get() = "!"

    override final fun equals(@EqualityBound(AC::class) other: Any?) = true
}

value object A2 : AC() {
    val p: String get() = "!"
}

// MODULE: m2(m1)
// FILE: f2.kt
package p

fun test(
    any: Any?,
    a0: A0, a1: A1, a2: A2,
    b0: B0, b1: B1, b2: B2,
) {
    if (a0 == any) any.p
    if (a1 == any) any.<!UNRESOLVED_REFERENCE!>p<!>
    if (b0 == any) any.p
    if (b1 == any) any.<!UNRESOLVED_REFERENCE!>p<!>
    if (a2 == any || b2 == any) any.q
    if (a2 == any) any.<!UNRESOLVED_REFERENCE!>p<!>
    if (b2 == any) any.<!UNRESOLVED_REFERENCE!>p<!>
}

value object B0 {
    val p: String get() = "!"
}

value object B1 {
    val p: String get() = "!"
    override fun equals(other: Any?) = other is B1
}

value object B2 : AC() {
    val p: String get() = "!"
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, getter, isExpression, nullableType,
objectDeclaration, operator, override, propertyDeclaration, stringLiteral, value */
