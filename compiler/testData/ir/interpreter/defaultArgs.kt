@CompileTimeCalculation
fun sum(a: Int = 1, b: Int = 2, c: Int = 3) = a + b + c

@CompileTimeCalculation
fun sumBasedOnPrevious(a: Int = 1, b: Int = a * 2, c: Int = b * 2) = a + b + c

@CompileTimeCalculation
interface A {
    fun foo(x: Int, y: Int = x + 20, z: Int = y * 2) = z
}

@CompileTimeCalculation
class B : A {}

const val sum1 = <!EVALUATED: `6`!>sum()<!>
const val sum2 = <!EVALUATED: `1`!>sum(b = -3)<!>
const val sum3 = <!EVALUATED: `3`!>sum(c = 1, a = 1, b = 1)<!>

const val sumBasedOnPrevious1 = <!EVALUATED: `7`!>sumBasedOnPrevious()<!>
const val sumBasedOnPrevious2 = <!EVALUATED: `3`!>sumBasedOnPrevious(b = 1, c = 1)<!>

const val sumInInterfaceDefault1 = B().<!EVALUATED: `42`!>foo(1)<!>
const val sumInInterfaceDefault2 = B().<!EVALUATED: `4`!>foo(x = 1, y = 2)<!>
const val sumInInterfaceDefault3 = B().<!EVALUATED: `-1`!>foo(x = 1, y = 2, z = -1)<!>

const val someConstProp = 0
@CompileTimeCalculation
class Outer {
    val prop = -1

    inner class Inner {
        val innerProp = -2

        fun withInner(x: Int = prop) = x
        fun withOuter(x: Int = innerProp) = x
        fun withGlobal(x: Int = someConstProp) = x
    }
}

const val inner1 = Outer().Inner().<!EVALUATED: `100`!>withInner(100)<!>
const val inner2 = Outer().Inner().<!EVALUATED: `-1`!>withInner()<!>
const val inner3 = Outer().Inner().<!EVALUATED: `100`!>withOuter(100)<!>
const val inner4 = Outer().Inner().<!EVALUATED: `-2`!>withOuter()<!>
const val inner5 = Outer().Inner().<!EVALUATED: `100`!>withGlobal(100)<!>
const val inner6 = Outer().Inner().<!EVALUATED: `0`!>withGlobal()<!>

@CompileTimeCalculation
interface I<T> {
    val prop: T

    fun foo(x: T = prop): T
}

open class C<T> {
    open fun foo(x: T) = x
}

@CompileTimeCalculation
class D(override val prop: Int): C<Int>(), I<Int> {}

const val fooB1 = D(10).<!EVALUATED: `10`!>foo()<!>
const val fooB2 = D(10).<!EVALUATED: `-1`!>foo(-1)<!>
