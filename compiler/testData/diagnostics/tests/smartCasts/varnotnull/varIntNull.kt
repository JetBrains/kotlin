fun foo(): Int {
    var i: Int? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>42<!>
    i = null
    return i <!UNSAFE_INFIX_CALL!>+<!> 1
}