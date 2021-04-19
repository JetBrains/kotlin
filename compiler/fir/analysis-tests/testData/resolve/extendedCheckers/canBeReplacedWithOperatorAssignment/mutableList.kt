// WITH_RUNTIME

fun foo() {
    var listVar = mutableListOf(1, 2, 3)
    // now, Idea hightlights this code like error (cuz listVar
    // is mutable and listVar + 4 is immutable) and like warning
    // (cuz can be replaced with +=)
    <!ASSIGNED_VALUE_IS_NEVER_READ!>listVar<!> = <!ASSIGNMENT_TYPE_MISMATCH!>listVar + 4<!>
}
