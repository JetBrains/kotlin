data class A(val x: String) {
    val Int.x: Int get() = this
}

fun box(): String = A("OK").component1()
