fun testEquals(x: Int) {
    if (<!SENSELESS_COMPARISON!>x == null<!>) {}
    if (<!SENSELESS_COMPARISON!>x == (null)<!>) {}
    if (<!SENSELESS_COMPARISON!>x == foo@ null<!>) {}
}

fun testEqualsFlipped(x: Int) {
    if (<!SENSELESS_COMPARISON!>null == x<!>) {}
    if (<!SENSELESS_COMPARISON!>(null) == x<!>) {}
    if (<!SENSELESS_COMPARISON!>foo@ null == x<!>) {}
}

fun testNotEquals(x: Int) {
    if (<!SENSELESS_COMPARISON!>x != null<!>) {}
    if (<!SENSELESS_COMPARISON!>x != (null)<!>) {}
    if (<!SENSELESS_COMPARISON!>x != foo@ null<!>) {}
}

fun testNotEqualsFlipped(x: Int) {
    if (<!SENSELESS_COMPARISON!>null != x<!>) {}
    if (<!SENSELESS_COMPARISON!>(null) != x<!>) {}
    if (<!SENSELESS_COMPARISON!>foo@ null != x<!>) {}
}