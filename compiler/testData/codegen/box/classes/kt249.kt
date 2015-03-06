package x

class Outer() {
    class object {
        class Inner() {
        }
    }
}

fun box (): String {
    val inner = Outer.Default.Inner()
    return "OK"
}
