// IGNORE_BACKEND: JS_IR
fun box(): String {
    42!!
    42.toLong()!!
    return "OK"!!
}
