fun test() {
    var res : Boolean = true
    res = (res and false)
    res = (res or false)
    <!UNUSED_VALUE!>res =<!> (res xor false)
    <!UNUSED_VALUE!>res =<!> (true and false)
    <!UNUSED_VALUE!>res =<!> (true or false)
    <!UNUSED_VALUE!>res =<!> (true xor false)
    <!UNUSED_VALUE!>res =<!> (!true)
    <!UNUSED_VALUE!>res =<!> (true && false)
    <!UNUSED_VALUE!>res =<!> (true || false)
}
