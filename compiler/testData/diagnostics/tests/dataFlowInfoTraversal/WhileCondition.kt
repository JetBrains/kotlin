fun simpleWhile(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        x : Int
        y++
    }
    x : Int
}

fun whileWithBreak(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        x : Int
        break
    }
    x : Int
}

fun whileWithNoCondition(x: Int?) {
    while (<!SYNTAX!><!>) {
        x!!
    }
    <!TYPE_MISMATCH!>x<!> : Int
}
