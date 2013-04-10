package lvalue_assignment

open class B() {
    var b: Int = 2
    val c: Int = 34
}

class C() : B() {
    var x = 4
    fun foo(<!UNUSED_PARAMETER!>c<!>: C) {
        this.x = 34
        this.b = 123
        super.b = 23
        <!VAL_REASSIGNMENT!>this.c<!> = 34
        <!VAL_REASSIGNMENT!>super.c<!> = 3535 //repeat for 'c'

        <!VARIABLE_EXPECTED!>getInt()<!> = 12
    }

    fun foo1(<!UNUSED_PARAMETER!>c<!>: C) {
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
    var <!UNUSED_VARIABLE!>i<!>: Int = 5

    <!UNRESOLVED_REFERENCE!>z<!> = 30;
    <!VARIABLE_EXPECTED!>""<!> = "";
    <!VARIABLE_EXPECTED!>foo()<!> = Unit.VALUE;

    <!USELESS_PARENTHESES!>(<!VARIABLE_EXPECTED!>i <!USELESS_CAST!>as<!> Int<!>)<!> = 34
    <!USELESS_PARENTHESES!>(<!VARIABLE_EXPECTED!>i is Int<!>)<!> = false
    <!VARIABLE_EXPECTED!>A()<!> = A()
    <!VARIABLE_EXPECTED!>5<!> = 34
}

fun canBe(i0: Int, j: Int) {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>i<!> = i0
    <!USELESS_PARENTHESES!>(i: Int)<!> = <!UNUSED_VALUE!>36<!>
    <!USELESS_PARENTHESES!>(@label i)<!> = <!UNUSED_VALUE!>34<!>

    <!USELESS_PARENTHESES!>(<!VAL_REASSIGNMENT!>j<!>: Int)<!> = <!UNUSED_VALUE!>36<!>
    <!USELESS_PARENTHESES!>(@label j)<!> = <!UNUSED_VALUE!>34<!> //repeat for j

    val a = A()
    <!USELESS_PARENTHESES!>(@ a.a)<!> = 3894
}

fun canBe2(j: Int) {
    <!USELESS_PARENTHESES!>(@label <!VAL_REASSIGNMENT!>j<!>)<!> = <!UNUSED_VALUE!>34<!>
}

class A() {
    var a: Int = 3
}

class Test() {
    fun testIllegalValues() {
        <!VARIABLE_EXPECTED!>1<!> += 23
        <!USELESS_PARENTHESES!>(<!VARIABLE_EXPECTED!>1<!> : Int)<!> += 43
        <!USELESS_PARENTHESES!>(@l <!VARIABLE_EXPECTED!>1<!>)<!> += 23

        <!VARIABLE_EXPECTED!>getInt()<!> += 343
        <!USELESS_PARENTHESES!>(@f <!VARIABLE_EXPECTED!>getInt()<!>)<!> += 343
        <!USELESS_PARENTHESES!>(<!VARIABLE_EXPECTED!>getInt()<!> : Int)<!> += 343

        <!VARIABLE_EXPECTED!>1<!>++
        (@r <!VARIABLE_EXPECTED!>1<!>)++
        (<!VARIABLE_EXPECTED!>1<!> : Int)++

        <!VARIABLE_EXPECTED!>getInt()<!>++
        (@m <!VARIABLE_EXPECTED!>getInt()<!>)++
        (<!VARIABLE_EXPECTED!>getInt()<!> : Int)++

        this<!UNRESOLVED_REFERENCE!>++<!>

        var s : String = "r"
        s += "ss"
        s += this
        s += <!USELESS_PARENTHESES!>(@a 2)<!>
    }

    fun testVariables() {
        var a: Int = 34
        val b: Int = 34

        a += 34
        <!USELESS_PARENTHESES!>(@l a)<!> += 34
        <!USELESS_PARENTHESES!>(a : Int)<!> += 34

        <!VAL_REASSIGNMENT!>b<!> += 34

        a++
        (@ a)++
        (a : Int)++
        <!USELESS_PARENTHESES!>(a)<!>++
    }

    fun testVariables1() {
        val b: Int = 34

        <!USELESS_PARENTHESES!>(@l <!VAL_REASSIGNMENT!>b<!>)<!> += 34
        //repeat for b
        <!USELESS_PARENTHESES!>(b : Int)<!> += 34
        <!USELESS_PARENTHESES!>(b)<!> += 3
    }

    fun testArrays(a: Array<Int>, ab: Ab) {
        a[3] = 4
        a[4]++
        a[6] += 43

        <!VARIABLE_EXPECTED!>ab.getArray()<!>[54] = 23
        <!VARIABLE_EXPECTED!>ab.getArray()<!>[54]++

        (@f a)[3] = 4
        (a : Array<Int>)[4]++
        (<!VARIABLE_EXPECTED!>ab.getArray()<!> : Array<Int>)[54] += 43

        this<!NO_SET_METHOD!><!UNRESOLVED_REFERENCE!>[<!>54]<!> = 34
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
