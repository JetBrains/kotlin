// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String {
    var x = ""
    run { x = "OK" }
    return x
}