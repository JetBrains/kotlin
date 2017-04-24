// WITH_RUNTIME

fun box(): String {
    var sum = 0
    for (c in generate()) {
        sum += (c.toInt() - '0'.toInt())
    }

    if (sum != 14) {
        return "Fail"
    }

    var x = 0
    for (c in (++x).toString()) {
        if (c != '1') {
            return "Fail"
        }
    }

    return "OK"
}

var invocationCounter = 0
fun generate(): String {
    ++invocationCounter
    assert(invocationCounter == 1)
    return "239"
}
