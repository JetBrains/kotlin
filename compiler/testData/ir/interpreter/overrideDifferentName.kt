@CompileTimeCalculation
open class A {
    open fun inc(i: Int) = i + 1
}

@CompileTimeCalculation
class B(val b: Int) : A() {
    override fun inc(j: Int): Int {
        return j + b
    }
}

const val a = <!EVALUATED: `11`!>A().inc(10)<!>
const val b = <!EVALUATED: `21`!>B(10).inc(11)<!>
