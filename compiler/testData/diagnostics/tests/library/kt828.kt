fun test() {
    var res : Boolean = true
    res = res and false
    res = res or false
    res = <!UNUSED_VALUE!>res xor false<!>
    res = <!UNUSED_VALUE!>true and false<!>
    res = <!UNUSED_VALUE!>true or false<!>
    res = <!UNUSED_VALUE!>true xor false<!>
    res = <!UNUSED_VALUE!>!true<!>
    res = <!UNUSED_VALUE!>true && false<!>
    res = <!UNUSED_VALUE!>true || false<!>
}
