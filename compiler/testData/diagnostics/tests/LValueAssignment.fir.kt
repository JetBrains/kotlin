// RUN_PIPELINE_TILL: FRONTEND

package lvalue_assignment

open class B() {
    var b: Int = 2
    val c: Int = 34
}

class C() : B() {
    var x = 4
    fun foo(c: C) {
        this.x = 34
        this.b = 123
        super.b = 23
        this.<!VAL_REASSIGNMENT!>c<!> = 34
        super.<!VAL_REASSIGNMENT!>c<!> = 3535 //repeat for 'c'

        <!VARIABLE_EXPECTED!>getInt()<!> = 12
    }

    fun foo1(c: C) {
        super.<!VAL_REASSIGNMENT!>c<!> = 34
    }

    fun bar(c: C) {
        <!VARIABLE_EXPECTED!>this<!> = c  //should be an error
    }
}

fun getInt() = 0

class D() {
    inner class B() {
        fun foo() {
            <!VARIABLE_EXPECTED!>this@D<!> = D()
        }
    }
}

fun foo(): Unit {}

fun cannotBe() {
    var i: Int = 5

    <!UNRESOLVED_REFERENCE!>z<!> = 30;
    <!VARIABLE_EXPECTED!>""<!> = "";
    <!VARIABLE_EXPECTED!>foo()<!> = Unit;

    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(<!VARIABLE_EXPECTED!>i <!USELESS_CAST!>as Int<!><!>)<!> = 34
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(<!USELESS_IS_CHECK, VARIABLE_EXPECTED!>i is Int<!>)<!> = false
    <!VARIABLE_EXPECTED!>A()<!> = A()
    <!VARIABLE_EXPECTED!>5<!> = 34
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Ann

fun canBe(i0: Int, j: Int) {
    var i = i0
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(label@ i)<!> = 34

    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(label@ <!VAL_REASSIGNMENT!>j<!>)<!> = 34 //repeat for j

    val a = A()
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(l@ a.a)<!> = 3894

    @Ann
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>l@ (i)<!> = 123
}

fun canBe2(j: Int) {
    <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(label@ <!VAL_REASSIGNMENT!>j<!>)<!> = 34
}

class A() {
    var a: Int = 3
}

class Test() {
    fun testIllegalValues() {
        <!VARIABLE_EXPECTED!>1<!> += 23
        (l@ 1) <!UNRESOLVED_REFERENCE!>+=<!> 23

        <!VARIABLE_EXPECTED!>getInt()<!> += 343
        (f@ getInt()) <!UNRESOLVED_REFERENCE!>+=<!> 343

        <!VARIABLE_EXPECTED!>1<!>++
        <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(r@ <!VARIABLE_EXPECTED!>1<!>)<!>--

        <!VARIABLE_EXPECTED!>getInt()<!>++
        <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(m@ <!VARIABLE_EXPECTED!>getInt()<!>)<!>--

        ++<!VARIABLE_EXPECTED!>2<!>
        --<!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(r@ <!VARIABLE_EXPECTED!>2<!>)<!>

        <!VARIABLE_EXPECTED!>this<!><!UNRESOLVED_REFERENCE!>++<!>

        var s : String = "r"
        s += "ss"
        s += this
        s += (a@ 2)

        @Ann
        <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>l@ (<!VARIABLE_EXPECTED!>1<!>)<!> = 123
    }

    fun testIllegalTypeRef(): Any {
        <!VARIABLE_EXPECTED!>Char<!>=
            return ""
    }

    fun testIncompleteSyntax() {
        val s = "s"
        <!UNRESOLVED_REFERENCE!>++<!><!VARIABLE_EXPECTED!>s<!>.<!SYNTAX!><!>
    }

    fun testVariables() {
        var a: Int = 34
        val b: Int = 34

        a += 34
        (l@ a) <!UNRESOLVED_REFERENCE!>+=<!> 34

        <!VAL_REASSIGNMENT!>b<!> += 34

        a++
        <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(@Ann l@ a)<!>--
        <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(a)<!>++
        --a
        ++<!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(@Ann l@ a)<!>
        --<!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(a)<!>
    }

    fun testVariables1() {
        val b: Int = 34

        (l@ b) <!UNRESOLVED_REFERENCE!>+=<!> 34
        //repeat for b
        (b) <!UNRESOLVED_REFERENCE!>+=<!> 3
    }

    fun testArrays(a: Array<Int>, ab: Ab) {
        a[3] = 4
        a[4]++
        a[6] += 43
        @Ann
        a[7] = 7
        (@Ann l@ (a))[8] = 8

        ab.getArray()[54] = 23
        ab.getArray()[54]++

        (f@ a)[3] = 4

        this<!NO_SET_METHOD!>[54]<!> = 34
    }
}

fun Array<Int>.checkThis() {
    this[45] = 34
    this[352]++
    this[35] += 234
}

abstract class Ab {
    abstract fun getArray() : Array<Int>
}
