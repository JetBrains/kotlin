abstract class A @CompileTimeCalculation constructor() {
    @CompileTimeCalculation
    abstract fun getIntNum(): Int

    @CompileTimeCalculation
    abstract fun getIntNumInverse(): Int
}

abstract class B @CompileTimeCalculation constructor(@CompileTimeCalculation val b: Int) : A() {
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

    @CompileTimeCalculation
    override fun getIntNumInverse(): Int {
        return -c
    }
}

@CompileTimeCalculation
fun getClassCAsA(num: Int): A {
    return C(num)
}

@CompileTimeCalculation
fun getClassCAsB(num: Int): B {
    return C(num)
}

// all methods call from C
const val num1 = <!EVALUATED: `1`!>getClassCAsA(1).getIntNum()<!>
const val num2 = <!EVALUATED: `2`!>getClassCAsB(2).getIntNum()<!>

const val num3 = <!EVALUATED: `-3`!>getClassCAsA(3).getIntNumInverse()<!>
const val num4 = <!EVALUATED: `-4`!>getClassCAsB(4).getIntNumInverse()<!>
