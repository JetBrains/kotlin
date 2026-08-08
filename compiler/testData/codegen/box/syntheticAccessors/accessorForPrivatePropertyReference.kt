// ISSUE: KT-88228

open class A {
    private val aVal = "O"
    private var aVar = ""

    class B : A() {
        val bVal by A::aVal
        var bVar by A::aVar
    }
}

fun box(): String {
    val b = A.B()
    b.bVar = "K"
    return b.bVal + b.bVar
}
