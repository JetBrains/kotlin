fun foo(): Int {
    var i: Int? = 42
    i = null
    return <!RETURN_TYPE_MISMATCH!>i + 1<!>
}
