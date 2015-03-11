package x

class Outer() {
    default object {
        class Inner() {
        }
    }
}

fun box (): String {
    val inner = Outer.Default.Inner()
    return "OK"
}
