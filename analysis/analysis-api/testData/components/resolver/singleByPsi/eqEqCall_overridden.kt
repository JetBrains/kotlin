open class A {
    override fun equals(other: Any?): Boolean {
        return true
    }
}

fun test(a1: A, a2: A) {
    <expr>a1 == a2</expr>
}