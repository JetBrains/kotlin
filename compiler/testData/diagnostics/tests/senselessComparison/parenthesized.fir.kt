fun testEquals(x: Int) {
    if (x == null) {}
    if (x == (null)) {}
    if (x == foo@ null) {}
}

fun testEqualsFlipped(x: Int) {
    if (null == x) {}
    if ((null) == x) {}
    if (foo@ null == x) {}
}

fun testNotEquals(x: Int) {
    if (x != null) {}
    if (x != (null)) {}
    if (x != foo@ null) {}
}

fun testNotEqualsFlipped(x: Int) {
    if (null != x) {}
    if ((null) != x) {}
    if (foo@ null != x) {}
}