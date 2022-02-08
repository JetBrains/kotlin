// WITH_STDLIB
fun foo() {
    var list = listOf(1, 2, 3)
    // Should not be highlighted because it's the way we use to say explicitly
    // "yes, we want to re-assign this immutable list"
    <!ASSIGNED_VALUE_IS_NEVER_READ!>list<!> = list + 4
}
