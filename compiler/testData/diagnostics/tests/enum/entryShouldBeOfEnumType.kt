enum class E {
    E1,
    E2
}

fun foo() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>e<!> = E.E1
    e = <!UNUSED_VALUE!>E.E2<!>
}