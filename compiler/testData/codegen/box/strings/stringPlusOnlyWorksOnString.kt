// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String {
    var x: MutableCollection<Int> = ArrayList()
    x + ArrayList()
    return "OK"
}
