open class Outer(val outer: String) {
    open inner class Inner(val inner: String): Outer(inner) {
        fun foo() = outer
    }

    fun value() = Inner("OK").foo()
}

fun box() = Outer("Fail").value()

fun main(args : Array<String>) {
    println(box())
}