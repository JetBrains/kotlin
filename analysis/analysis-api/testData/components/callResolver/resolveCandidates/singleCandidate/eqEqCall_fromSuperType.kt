open class A {
    override fun equals(other: Any?): Boolean {
        return true
    }
}

class B : A()

fun test(b1: B, b2: B) {
    <expr>b1 == b2</expr>
}