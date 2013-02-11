fun bar1(x: Number, y: Int) {
    var yy = y
    yy += x as Int
    x : Int
}

fun bar2(x: Number) {
    <!UNRESOLVED_REFERENCE!>y<!> <!UNRESOLVED_REFERENCE!>+=<!> x as Int
    x : Int
}

fun bar3(x: Number, y: Array<Int>) {
    y[0] += x as Int
    x : Int
}
