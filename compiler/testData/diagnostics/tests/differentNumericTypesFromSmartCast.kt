// ISSUE: KT-22499

fun test(x: Any, y: Any) =
    x is Float && y is Double && x == y

fun test(x: Float, y: Double) =
    <!EQUALITY_NOT_APPLICABLE!>x == y<!>
