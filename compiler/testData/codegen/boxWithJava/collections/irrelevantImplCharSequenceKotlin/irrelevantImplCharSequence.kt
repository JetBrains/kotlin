abstract class AImpl {
    fun charAt(index: Int): Char {
        return 'A'
    }

    fun length(): Int {
        return 56
    }
}

class X : J.A()

fun box(): String {
    val x = X()
    if (x.length != 56) return "fail 1"
    if (x[0] != 'A') return "fail 2"
    return "OK"
}
