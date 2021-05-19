open class A @CompileTimeCalculation constructor(@CompileTimeCalculation var a: Int) {
    @CompileTimeCalculation
    fun get(): Int {
        return a
    }

    @CompileTimeCalculation
    open fun openGet(): Int {
        return a
    }

    @CompileTimeCalculation
    fun setA(a: Int): A {
        this.a = a
        return this
    }
}

open class B @CompileTimeCalculation constructor(@CompileTimeCalculation val b: Int) {
    @CompileTimeCalculation val aObj = A(b + 1)

    @CompileTimeCalculation
    fun getAFromB(): Int {
        return aObj.a
    }

    @CompileTimeCalculation
    fun getFromProperty(): Int {
        return aObj.get()
    }
}

open class C @CompileTimeCalculation constructor(@CompileTimeCalculation val c: Int) {
    @CompileTimeCalculation val aObj = A(c + 2)
    @CompileTimeCalculation val bObj = B(c + 1)

    @CompileTimeCalculation
    fun getAFromC(): Int {
        return aObj.a
    }

    @CompileTimeCalculation
    fun getBFromC(): Int {
        return bObj.b
    }

    @CompileTimeCalculation
    fun openGet(): Int {
        return aObj.openGet()
    }
}

const val a1 = A(1).<!EVALUATED: `1`!>get()<!>
const val a2 = A(1).setA(2).<!EVALUATED: `2`!>get()<!>
const val a3 = A(1).<!EVALUATED: `1`!>openGet()<!>

const val b1 = B(1).<!EVALUATED: `2`!>getAFromB()<!>
const val b2 = B(1).<!EVALUATED: `2`!>getFromProperty()<!>
const val b3 = B(1).aObj.<!EVALUATED: `2`!>get()<!>
const val b4 = B(1).aObj.setA(-1).<!EVALUATED: `-1`!>get()<!>
const val b5 = B(1).aObj.<!EVALUATED: `2`!>a<!>

const val c1 = C(1).<!EVALUATED: `3`!>getAFromC()<!>
const val c2 = C(1).<!EVALUATED: `2`!>getBFromC()<!>
const val c3 = C(1).aObj.<!EVALUATED: `3`!>get()<!>
const val c4 = C(1).<!EVALUATED: `3`!>openGet()<!>
const val c5 = C(1).bObj.<!EVALUATED: `3`!>getAFromB()<!>
const val c6 = C(1).bObj.<!EVALUATED: `3`!>getFromProperty()<!>
const val c7 = C(1).bObj.aObj.setA(-2).<!EVALUATED: `-2`!>get()<!>
const val c8 = C(1).bObj.<!EVALUATED: `2`!>b<!>
const val c9 = C(1).aObj.<!EVALUATED: `3`!>a<!>
