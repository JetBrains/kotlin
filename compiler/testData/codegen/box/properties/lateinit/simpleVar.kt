// IGNORE_BACKEND: JS_IR
class A {
    public lateinit var str: String
}

fun box(): String {
    val a = A()
    a.str = "OK"
    return a.str
}