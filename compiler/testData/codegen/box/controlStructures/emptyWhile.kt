// IGNORE_BACKEND: JS_IR
fun box(): String {
    while (false);

    var x = 0
    while (x++<5);
    if (x != 6) return "Fail: $x"

    return "OK"
}
