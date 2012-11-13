fun simpleWhile(x: Int?, var y: Int) {
    while (x!! == y) {
        x : Int
        y++
    }
    x : Int
}

fun whileWithBreak(x: Int?, var y: Int) {
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
