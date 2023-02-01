// LANGUAGE: +ProhibitConfusingSyntaxInWhenBranches
// DIAGNOSTICS: -INCOMPATIBLE_TYPES, -NON_EXHAUSTIVE_WHEN_STATEMENT, -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// ISSUE: KT-48385

operator fun Boolean.plus(other: Boolean): Boolean = true
operator fun Boolean.minus(other: Boolean): Boolean = true
operator fun Boolean.times(other: Boolean): Boolean = true
operator fun Boolean.div(other: Boolean): Boolean = true
operator fun Boolean.rem(other: Boolean): Boolean = true

operator fun Boolean.rangeTo(other: Boolean): Boolean = true

fun Boolean.id(): Boolean = true

operator fun Boolean.inc(): Boolean = true
operator fun Boolean.dec(): Boolean = true

operator fun Boolean.plusAssign(other: Boolean) {}
operator fun Boolean.minusAssign(other: Boolean) {}
operator fun Boolean.timesAssign(other: Boolean) {}
operator fun Boolean.divAssign(other: Boolean) {}
operator fun Boolean.remAssign(other: Boolean) {}

operator fun Any?.contains(other: Any): Boolean = false

fun testWithSubject_ok(x: Boolean, y: Boolean?, any: Any, z: Boolean) {
    when {
        x.id() -> {}
        <!CONDITION_TYPE_MISMATCH!>y?.id()<!> -> {}
        <!CONDITION_TYPE_MISMATCH!>any as? Boolean<!> -> {}
        any as Boolean -> {}
        x * x -> {}
        x / x -> {}
        x % x -> {}
        x + x -> {}
        x - x -> {}
        x..x -> {}
        <!CONDITION_TYPE_MISMATCH!>y<!> -> {}
        y ?: x -> {}
        x in x -> {}
        x !in x -> {}
        x is String -> {}
        x !is String -> {}
        x <!OVERLOAD_RESOLUTION_AMBIGUITY!><<!> x -> {}
        x <!OVERLOAD_RESOLUTION_AMBIGUITY!>><!> x -> {}
        x <!OVERLOAD_RESOLUTION_AMBIGUITY!><=<!> x -> {}
        x <!OVERLOAD_RESOLUTION_AMBIGUITY!>>=<!> x -> {}
        x == x -> {}
        x != x -> {}
        <!DEPRECATED_IDENTITY_EQUALS!>x === x<!> -> {}
        <!DEPRECATED_IDENTITY_EQUALS!>x !== x<!> -> {}
        x && x -> {}
        x || x -> {}
    }

    var b = z
    <!NO_ELSE_IN_WHEN!>when<!> (z) {
        b++ -> {}
        b-- -> {}
    }
}
