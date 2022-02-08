fun foo(something: Boolean) {
    var res = false
    <!ASSIGNED_VALUE_IS_NEVER_READ!>res<!> = res and something
}