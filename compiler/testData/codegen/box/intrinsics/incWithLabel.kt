// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun box(): String {
    var x = 1
    (foo@ x)++
    ++(foo@ x)

    if (x != 3) return "Fail: $x"
    return "OK"
}