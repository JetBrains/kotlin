abstract class A @CompileTimeCalculation constructor() {
    @CompileTimeCalculation
    abstract fun getIntNum(): Int
}

open class B @CompileTimeCalculation constructor(@CompileTimeCalculation val b: Int) : A() {
    @CompileTimeCalculation
    override fun getIntNum(): Int {
        return b
    }
}

class C @CompileTimeCalculation constructor(@CompileTimeCalculation val c: Int) : B(c + 1) {
    @CompileTimeCalculation
    override fun getIntNum(): Int {
        return c
    }
}

@CompileTimeCalculation
fun getAClassImplementation(num: Int): A {
    return B(num)
}

@CompileTimeCalculation
fun getBClassImplementation(num: Int): B {
    return B(num)
}

@CompileTimeCalculation
fun getClassCAsA(num: Int): A {
    return C(num)
}

@CompileTimeCalculation
fun getClassCAsB(num: Int): B {
    return C(num)
}

@CompileTimeCalculation
fun getClassCAsC(num: Int): C {
    return C(num)
}

const val num1 = <!EVALUATED: `1`!>getAClassImplementation(1).getIntNum()<!>
const val num2 = <!EVALUATED: `2`!>getBClassImplementation(2).getIntNum()<!>

// all `getIntNum` methods are from class C
const val num3 = <!EVALUATED: `3`!>getClassCAsA(3).getIntNum()<!>
const val num4 = <!EVALUATED: `4`!>getClassCAsB(4).getIntNum()<!>
const val num5 = <!EVALUATED: `5`!>getClassCAsC(5).getIntNum()<!>
