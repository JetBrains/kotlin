// CHECK_TYPE

fun bar1(x: Number, y: Int) {
    var yy = y
    yy += x as Int
    checkSubtype<Int>(x)
}

fun bar2(x: Number) {
    <!UNRESOLVED_REFERENCE!>y<!> += x as Int
    checkSubtype<Int>(x)
}

fun bar3(x: Number, y: Array<Int>) {
    y[0] += x as Int
    checkSubtype<Int>(x)
}
