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
        <!VAL_REASSIGNMENT!>this.c<!> = 34
        <!VAL_REASSIGNMENT!>super.c<!> = 3535 //repeat for 'c'

        <!VARIABLE_EXPECTED!>getInt()<!> = 12
    }

    fun foo1(c: C) {
        <!VAL_REASSIGNMENT!>super.c<!> = 34
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

    (<!VARIABLE_EXPECTED!>i <!USELESS_CAST!>as Int<!><!>) = 34
    (<!USELESS_IS_CHECK, VARIABLE_EXPECTED!>i is Int<!>) = false
    <!VARIABLE_EXPECTED!>A()<!> = A()
    <!VARIABLE_EXPECTED!>5<!> = 34
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Ann

fun canBe(i0: Int, j: Int) {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>i<!> = i0
    (label@ i) = 34

    (label@ <!VAL_REASSIGNMENT!>j<!>) = 34 //repeat for j

    val a = A()
    (l@ a.a) = 3894

    @Ann
    l@ (i) = 123
}

fun canBe2(j: Int) {
    (label@ <!VAL_REASSIGNMENT!>j<!>) = 34
}

class A() {
    var a: Int = 3
}

class Test() {
    fun testIllegalValues() {
        <!VARIABLE_EXPECTED!>1<!> += 23
        (l@ <!VARIABLE_EXPECTED!>1<!>) += 23

        <!VARIABLE_EXPECTED!>getInt()<!> += 343
        (f@ <!VARIABLE_EXPECTED!>getInt()<!>) += 343

        <!VARIABLE_EXPECTED!>1<!>++
        (<!REDUNDANT_LABEL_WARNING!>r@<!> <!VARIABLE_EXPECTED!>1<!>)--

        <!VARIABLE_EXPECTED!>getInt()<!>++
        (<!REDUNDANT_LABEL_WARNING!>m@<!> <!VARIABLE_EXPECTED!>getInt()<!>)--

        ++<!VARIABLE_EXPECTED!>2<!>
        --(<!REDUNDANT_LABEL_WARNING!>r@<!> <!VARIABLE_EXPECTED!>2<!>)

        this<!UNRESOLVED_REFERENCE!>++<!>

        var s : String = "r"
        s += "ss"
        s += this
        s += (<!REDUNDANT_LABEL_WARNING!>a@<!> 2)

        @Ann
        l@ (<!VARIABLE_EXPECTED!>1<!>) = 123
    }

    fun testIncompleteSyntax() {
        val s = "s"
        <!DEBUG_INFO_MISSING_UNRESOLVED!>++<!>s.<!SYNTAX!><!>
    }

    fun testVariables() {
        var a: Int = 34
        val b: Int = 34

        a += 34
        (l@ a) += 34

        <!VAL_REASSIGNMENT!>b<!> += 34

        a++
        (@Ann <!REDUNDANT_LABEL_WARNING!>l@<!> a)--
        (a)++
        --a
        ++(@Ann <!REDUNDANT_LABEL_WARNING!>l@<!> a)
        --(a)
    }

    fun testVariables1() {
        val b: Int = 34

        (l@ <!VAL_REASSIGNMENT!>b<!>) += 34
        //repeat for b
        (b) += 3
    }

    fun testArrays(a: Array<Int>, ab: Ab) {
        a[3] = 4
        a[4]++
        a[6] += 43
        @Ann
        a[7] = 7
        (@Ann <!REDUNDANT_LABEL_WARNING!>l@<!> (a))[8] = 8

        ab.getArray()[54] = 23
        ab.getArray()[54]++

        (<!REDUNDANT_LABEL_WARNING!>f@<!> a)[3] = 4

        this<!NO_SET_METHOD!><!UNRESOLVED_REFERENCE!>[<!>54<!UNRESOLVED_REFERENCE!>]<!><!> = 34
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
