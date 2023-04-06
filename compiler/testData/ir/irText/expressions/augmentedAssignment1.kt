// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

var p = 0

fun testVariable() {
    var x = 0
    x += 1
    x -= 2
    x *= 3
    x /= 4
    x %= 5
}

fun testProperty() {
    p += 1
    p -= 2
    p *= 3
    p /= 4
    p %= 5
}
