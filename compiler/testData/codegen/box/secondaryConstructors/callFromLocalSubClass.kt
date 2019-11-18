// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val z = "K"
    open class A(val x: String) {
        constructor() : this("O")

        val y: String
            get() = z
    }

    class B : A()

    val b = B()

    return b.x + b.y
}
