fun box(): String {
    var x = 1
    (foo@ x)++
    ++(foo@ x)
    (x: Int)++
    ++(x: Int)

    if (x != 5) return "Fail: $x"
    return "OK"
}