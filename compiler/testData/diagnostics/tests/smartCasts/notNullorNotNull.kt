fun bar(x: Int?): Int {
    if (x != null) return -1
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>) return -2
    // Should be unreachable
    return 2 + 2
}