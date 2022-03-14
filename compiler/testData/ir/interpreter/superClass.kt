open class A @CompileTimeCalculation constructor(var a: Int) {
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

open class B @CompileTimeCalculation constructor(val b: Int) : A(b + 1) {
    @CompileTimeCalculation
    fun getAFromB(): Int {
        return a
    }

    @CompileTimeCalculation
    fun getFromValueParameter(a: A): Int {
        return a.get()
    }
}

open class C @CompileTimeCalculation constructor(val c: Int) : B(c + 1) {
    @CompileTimeCalculation
    fun getAFromC(): Int {
        return a
    }

    @CompileTimeCalculation
    override fun openGet(): Int {
        return super.openGet()
    }
}

open class D @CompileTimeCalculation constructor(val d: Int) : C(d + 1) {

}

open class E @CompileTimeCalculation constructor(val e: Int) : D(e + 1) {

}

const val a1 = <!EVALUATED: `1`!>A(1).get()<!>
const val a2 = <!EVALUATED: `2`!>A(1).setA(2).get()<!>
const val a3 = <!EVALUATED: `1`!>A(1).openGet()<!>
const val a4 = <!EVALUATED: `1`!>A(1).a<!>       // property inherits compile-time annotation from primary constructor

const val b1 = <!EVALUATED: `2`!>B(1).getAFromB()<!>
const val b2 = <!EVALUATED: `10`!>B(1).getFromValueParameter(A(10))<!>
const val b3 = <!EVALUATED: `2`!>B(1).get()<!>   //fake overridden
const val b4 = <!EVALUATED: `-1`!>B(1).setA(-1).get()<!>

const val c1 = <!EVALUATED: `3`!>C(1).getAFromC()<!>
const val c2 = <!EVALUATED: `3`!>C(1).get()<!>   //fake overridden
const val c3 = <!EVALUATED: `3`!>C(1).openGet()<!>
const val c4 = <!EVALUATED: `3`!>C(1).getAFromB()<!>
const val c5 = <!EVALUATED: `10`!>C(1).getFromValueParameter(A(10))<!> //method from B
const val c6 = <!EVALUATED: `-2`!>C(1).setA(-2).get()<!>

// test deep fake overridden
const val d1 = <!EVALUATED: `4`!>D(1).get()<!>
const val e1 = <!EVALUATED: `5`!>E(1).get()<!>
