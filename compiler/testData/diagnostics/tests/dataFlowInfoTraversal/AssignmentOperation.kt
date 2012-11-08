fun bar1(x: Number, var y: Int) {
    y += x as Int
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
