
class X : J.A()

fun box(): String {
    val x = X()
    if (x.size != 56) return "fail 1"
    if (!x.contains("")) return "fail 2"

    return "OK"
}
