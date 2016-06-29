// !DIAGNOSTICS: -UNUSED_PARAMETER

class Example {
    public fun plus(o: Example) = o
    public operator fun minus(o: Example) = o

    public fun get(i: Int) = ""
    public operator fun get(s: String) = ""

    public fun set(i: Int, v: String) {}
    public operator fun set(s: String, v: String) {}

    public fun not() = false

    public fun rangeTo(o: Example) = o
    public fun contains(o: Example) = false
    public fun compareTo(o: Example) = 0

    public fun inc() = this
    public fun dec() = this

    public fun invoke() {}
}

class Example2 {
    public operator fun not() = true

    public fun plusAssign(o: Example2) {}
    public operator fun minusAssign(o: Example2) {}

    public operator fun rangeTo(o: Example2) = o
    public operator fun contains(o: Example2) = false
    public operator fun compareTo(o: Example2) = 0

    public operator fun inc() = this
    public operator fun dec() = this

    public operator fun invoke() {}
}

fun test() {
    var a = Example()
    var b = Example()
    var c = Example2()
    var d = Example2()

    Example() == Example()

    a == b
    c != d

    Example() <!OPERATOR_MODIFIER_REQUIRED!>+<!> Example()

    a <!OPERATOR_MODIFIER_REQUIRED!>+<!> b
    a - b
    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!>
    a["str"]

    <!OPERATOR_MODIFIER_REQUIRED!>a[1]<!> = "A"
    a["str"] = "str"

    a.plus(b)
    a.minus(b)
    a.get(1)
    a.get("str")

    c <!OPERATOR_MODIFIER_REQUIRED!>+=<!> d
    c -= d

    a<!OPERATOR_MODIFIER_REQUIRED!>..<!>b
    c..d

    Example()<!OPERATOR_MODIFIER_REQUIRED!>..<!>Example()
    Example2()..Example2()

    a <!OPERATOR_MODIFIER_REQUIRED!><<!> b
    a <!OPERATOR_MODIFIER_REQUIRED!>>=<!> b
    c > d

    a <!OPERATOR_MODIFIER_REQUIRED!>in<!> b
    c in d

    a<!OPERATOR_MODIFIER_REQUIRED!>++<!>
    a<!OPERATOR_MODIFIER_REQUIRED!>--<!>
    c++
    c--

    <!OPERATOR_MODIFIER_REQUIRED!>!<!>a
    !c

    <!OPERATOR_MODIFIER_REQUIRED!>a<!>()
    c()

    <!OPERATOR_MODIFIER_REQUIRED!>Example()()<!>
    Example2()()
}

abstract class Base {
    abstract operator fun plus(o: Base): Base
    abstract fun minus(o: Base): Base
}

open class Anc : Base() {
    override fun plus(o: Base) = o
    override fun minus(o: Base) = o
}

class Anc2 : Anc()

fun test2() {
    Anc() + Anc()
    Anc() <!OPERATOR_MODIFIER_REQUIRED!>-<!> Anc()
    Anc2() + Anc2()
}