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

const val a = A().<!EVALUATED: `11`!>inc(10)<!>
const val b = B(10).<!EVALUATED: `21`!>inc(11)<!>
