enum class E {
    E1,
    E2
}

fun foo() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>e<!> = E.E1
    <!UNUSED_VALUE!>e =<!> E.E2
}