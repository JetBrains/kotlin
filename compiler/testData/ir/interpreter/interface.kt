interface A {
    @CompileTimeCalculation
    fun getStr(): String
}

interface B : A {
    @CompileTimeCalculation
    fun getInt(): Int
}

class C @CompileTimeCalculation constructor(@CompileTimeCalculation val num: Int) : B {
    @CompileTimeCalculation
    override fun getStr(): String {
        return num.toString()
    }

    @CompileTimeCalculation
    override fun getInt(): Int {
        return num
    }
}

@CompileTimeCalculation
fun getClassAsA(num: Int): A {
    return C(num)
}

@CompileTimeCalculation
fun getClassAsB(num: Int): B {
    return C(num)
}

@CompileTimeCalculation
fun getClassAsC(num: Int): C {
    return C(num)
}

const val num1 = getClassAsA(1).<!EVALUATED: `1`!>getStr()<!>
const val num2 = getClassAsB(2).<!EVALUATED: `2`!>getStr()<!>
const val num3 = getClassAsB(3).<!EVALUATED: `3`!>getInt()<!>
const val num4 = getClassAsC(4).<!EVALUATED: `4`!>getStr()<!>
const val num5 = getClassAsC(5).<!EVALUATED: `5`!>getInt()<!>
