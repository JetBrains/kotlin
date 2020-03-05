// !CHECK_TYPE

fun simpleWhile(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        checkSubtype<Int>(x)
        y++
    }
    checkSubtype<Int>(x)
}

fun whileWithBreak(x: Int?, y0: Int) {
    var y = y0
    while (x!! == y) {
        checkSubtype<Int>(x)
        break
    }
    checkSubtype<Int>(x)
}

fun whileWithNoCondition(x: Int?) {
    while (<!SYNTAX!><!>) {
        x!!
    }
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
}
