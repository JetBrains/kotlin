package x

class Outer() {
    companion object {
        class Inner() {
        }
    }
}

fun box (): String {
    val inner = Outer.Companion.Inner()
    return "OK"
}
