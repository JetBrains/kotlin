fun simpleFor() {
    var a = 1
    for(i in 1..3) {
        a = i
    }
    42
}

fun simpleWhile() {
    var a = 2
    while (a < 3) {
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

fun infiniteWhile() {
    var i = 1
    while (i > 0) ++i
}

fun restrictionsInWhile1() {
    for (i in 1..3) {
        while (i < 3) {
            42
        }
        while (i >= 2) {
            43
        }
    }
    44
}

fun restrictionsInWhile2() {
    var a = 1
    if ("".length() > 0) {
        a = 3
    }
    while (a < 3) {
        42
    }
    while (a >= 2) {
        43
    }
    44
}

fun whileWithUpdateInCondition() {
    var a = 0
    while (++a > 0) {
        42
    }
    43
}

fun notSimpleDoWhile() {
    var a = -1
    do {
        a = a + 1
    }
    while (a < 2)
    42
}