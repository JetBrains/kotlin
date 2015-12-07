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

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION!>class Test1<!> : Final(), IFoo by CFoo()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION!>class Test2<!> : Final(), IBar by CBar()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION, VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION!>class Test3<!> : Final(), IQux by CQux()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION!>class Test4<!> : Derived(), IFoo by CFoo()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION!>class Test5<!> : Derived(), IBar by CBar()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION, VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION!>class Test6<!> : Derived(), IQux by CQux()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION!>class Test7<!> : Final(), IBarT<Int> by CBarT<Int>()

<!OVERRIDING_FINAL_MEMBER_BY_DELEGATION!>class Test8<!> : Final(), IBarT<Int> by <!TYPE_MISMATCH!>CBar()<!>
