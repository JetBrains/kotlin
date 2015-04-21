// !CHECK_TYPE

fun bar1(x: Number, y: Int) {
    var yy = y
    yy += x as Int
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun bar2(x: Number) {
    <!UNRESOLVED_REFERENCE!>y<!> <!UNRESOLVED_REFERENCE!>+=<!> x as Int
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun bar3(x: Number, y: Array<Int>) {
    y[0] += x as Int
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
