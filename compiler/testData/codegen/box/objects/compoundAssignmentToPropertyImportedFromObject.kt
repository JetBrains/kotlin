import Host.x

object Host {
    var x = 0
}

fun box(): String {
    x += 1
    if (x != 1) return "Fail 1: $x"

    x++
    if (x != 2) return "Fail 2: $x"

    ++x
    if (x != 3) return "Fail 3: $x"

    return "OK"
}