fun foo(): Any? {
    [suppress("REDUNDANT_NULLABLE")]
    return null <!USELESS_CAST!>as<!> Nothing??
}