// LANGUAGE: +FullValueClasses
// WITH_STDLIB

// A typed `equals` is an overload, as in a regular class: `==` calls the generated `equals(Any?)`.
value class Single(val a: Int) {
    fun equals(other: Single) = true
}

value class Multi(val a: Int, val b: Int) {
    fun equals(other: Multi) = other.a == a
}

class Regular(val a: Int) {
    fun equals(other: Regular) = true
}

fun box(): String {
    if (Single(1) == Single(2) || !Single(1).equals(Single(2))) return "Fail: Single"
    if (Multi(1, 2) == Multi(1, 3) || (Multi(1, 2) as Any) == Multi(1, 3) || !Multi(1, 2).equals(Multi(1, 3))) return "Fail: Multi"
    if (Regular(1) == Regular(2) || !Regular(1).equals(Regular(2))) return "Fail: Regular"
    return "OK"
}
