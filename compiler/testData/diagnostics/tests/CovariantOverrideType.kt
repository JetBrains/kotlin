interface A {
    fun foo() : Int = 1
    fun foo2() : Int = 1
    fun foo1() : Int = 1
    val a : Int
    val a1 : Int
    val <T> g : Iterator<T>

    fun <T> g() : T
    fun <T> g1() : T
}

abstract class B() : A {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() {
    }
    override fun foo2() : <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Unit<!> {
    }

    override val a : <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Double<!> = 1.toDouble()
    override val <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>a1<!> = 1.toDouble()

    abstract override fun <X> g() : <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Int<!>
    abstract override fun <X> g1() : <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>List<X><!>

    abstract override val <X> g : <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Iterator<Int><!>
}