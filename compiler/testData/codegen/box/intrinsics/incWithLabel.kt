fun box(): String {
    var x = 1
    (foo@ x)++
    ++(foo@ x)

    if (x != 3) return "Fail: $x"
    return "OK"
}