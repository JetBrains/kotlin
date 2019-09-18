fun box(): String {
    var i = 10
    while (i > 0) {
        i = i - 1
    }

    do {
        i = i + 1
    } while (i <= 10)

    for (j in 0..i) {
        val t = j + 1
    }

    return "OK"
}