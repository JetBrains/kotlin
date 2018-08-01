open class B : A() {
    open inner class Inner : A.Inner()
}

fun main(args: Array<String>) {
    B().Inner()
}