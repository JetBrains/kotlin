abstract class A @CompileTimeCalculation constructor() {
    @CompileTimeCalculation
    abstract fun getInt(): Int
}

open class B @CompileTimeCalculation constructor(@CompileTimeCalculation val b: Int) : A() {
    @CompileTimeCalculation
    override fun getInt(): Int {
        return b
    }
}

abstract class C @CompileTimeCalculation constructor(@CompileTimeCalculation val c: Int) : B(c + 1) {
    @CompileTimeCalculation
    abstract fun getString(): String
}

class D @CompileTimeCalculation constructor(@CompileTimeCalculation val d: Int) : C(d + 1) {
    @CompileTimeCalculation
    override fun getString(): String {
        return d.toString()
    }
}
@CompileTimeCalculation
fun getClassDAsA(num: Int): A {
    return D(num)
}

@CompileTimeCalculation
fun getClassDAsB(num: Int): B {
    return D(num)
}

@CompileTimeCalculation
fun getClassDAsC(num: Int): C {
    return D(num)
}

@CompileTimeCalculation
fun getClassDAsD(num: Int): D {
    return D(num)
}

const val numA1 = <!EVALUATED: `3`!>getClassDAsA(1).getInt()<!>
const val numB1 = <!EVALUATED: `3`!>getClassDAsB(1).getInt()<!>
const val numC1 = <!EVALUATED: `3`!>getClassDAsC(1).getInt()<!>
const val numC2 = <!EVALUATED: `1`!>getClassDAsC(1).getString()<!>
const val numD1 = <!EVALUATED: `3`!>getClassDAsD(1).getInt()<!>
const val numD2 = <!EVALUATED: `1`!>getClassDAsD(1).getString()<!>
