fun simpleWhile(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
        y++
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}

fun whileWithBreak(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        <!DEBUG_INFO_AUTOCAST!>x<!> : Int
        break
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}

fun whileWithNoCondition(x: Int?) {
    while (<!SYNTAX!><!>) {
        x!!
    }
    <!TYPE_MISMATCH!>x<!> : Int
}
