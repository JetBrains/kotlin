// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class A() {

    override fun toString(): String {
        return "A"
    }
}

fun box() : String {

    val s = "1" + "2" + 3 + 4L + 5.0 + 6F + '7' + A()

    if (s != "12345.06.07A") return "fail $s"

    return "OK"
}

