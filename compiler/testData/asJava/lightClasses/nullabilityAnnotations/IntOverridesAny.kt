// C

interface Tr {
    fun foo(): Any
    val v: Any
}

class C: Tr {
    override fun foo() = 1
    override val v = { 1 }()
}