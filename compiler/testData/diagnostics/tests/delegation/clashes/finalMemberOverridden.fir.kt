// !WITH_NEW_INFERENCE
open class Final {
    fun foo() {}
    val bar: Int = 0
    var qux: Int = 0
}

open class Derived : Final()

interface IFoo {
    fun foo()
}

class CFoo : IFoo {
    override fun foo() {}
}

interface IBar {
    val bar: Int
}

class CBar : IBar {
    override val bar: Int get() = 0
}

interface IQux {
    val qux: Int
}

class CQux : IQux {
    override val qux: Int get() = 0
}

interface IBarT<T> {
    val bar: T
}

class CBarT<T> : IBarT<T> {
    override val bar: T get() = null!!
}

class Test1 : Final(), IFoo by CFoo()

class Test2 : Final(), IBar by CBar()

class Test3 : Final(), IQux by CQux()

class Test4 : Derived(), IFoo by CFoo()

class Test5 : Derived(), IBar by CBar()

class Test6 : Derived(), IQux by CQux()

class Test7 : Final(), IBarT<Int> by CBarT<Int>()

class Test8 : Final(), IBarT<Int> by CBar()
