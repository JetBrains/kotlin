class A {
    override fun equals(other: Any?): Boolean = true
}

fun test() {
    val a = A()
    val b = A()
    a == b
    a != b
    a === b
    a !== b
}
