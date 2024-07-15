// ISSUE: KT-69511

fun compareDynamicWithInt(n: dynamic): Boolean {
    return <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>n === 1<!>
}
