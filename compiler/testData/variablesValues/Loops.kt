fun simpleFor() {
    var a = 1
    for(i in 1..3) {
        a = i
    }
    42
}

fun simpleWhile() {
    var a = 2
    while (a < 2) {
    }
}

fun simpleDoWhile() {
    var a = 2
    do {
    }
    while (a < 4)
}

fun whileWithUpdate() {
    var a = 2
    var b = false
    val c: Int
    var d: Boolean
    while (a < 10) {
        a = a + 1
        b = !b
        c = 5
        d = true
        val e = 8
        var f = false
    }
    42
}

fun forWithUpdate() {
    var a = 2
    var b = false
    val c: Int
    var d: Boolean
    for (i in 1..10) {
        a = a + 1
        b = !b
        c = 5
        d = true
        var j = i
        val e = 8
        var f = false
    }
    42
}