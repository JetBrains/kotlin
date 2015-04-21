fun foo(): Any? {
    [suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")]
    return ""!! <!USELESS_CAST!>as String??<!>
}