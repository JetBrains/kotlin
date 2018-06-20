// IGNORE_BACKEND: JS_IR
//WITH_RUNTIME
fun box(): String {
    val x by lazy { "OK" }
    return x
}