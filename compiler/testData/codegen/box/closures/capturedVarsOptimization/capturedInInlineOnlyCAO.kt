// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String {
    var x = ""
    run {
        x += "O"
        x += "K"
    }
    return x
}