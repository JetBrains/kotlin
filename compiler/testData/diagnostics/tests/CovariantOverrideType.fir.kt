interface A<H> {
    fun foo() : Int = 1
    fun foo2() : Int = 1
    fun foo1() : Int = 1
    val a : Int
    val a1 : Int
    val g : Iterator<H>

    fun <T> g() : T
    fun <T> g1() : T
}

abstract class B<H>() : A<H> {
    override fun foo() {
    }
    override fun foo2() : Unit {
    }

    override val a : Double = 1.toDouble()
    override val a1 = 1.toDouble()

    abstract override fun <X> g() : Int
    abstract override fun <X> g1() : List<X>

    abstract override val g : Iterator<Int>
}