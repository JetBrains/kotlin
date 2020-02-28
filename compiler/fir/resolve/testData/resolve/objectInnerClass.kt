// !CHECK_TYPE
/*
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37120
 */

val case1 = object : A {
    inner class Child(property: B) : Base(property) {
        fun Base.zoo() {
            val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
        }

        fun foo() {
            <!UNRESOLVED_REFERENCE!>baseFun<!>() //UNRESOLVED_REFERENCE
            val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
            <!UNRESOLVED_REFERENCE!>zoo<!>() //UNRESOLVED_REFERENCE
            hoo()
        }
    }
    fun Child.voo() {
        val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
    }

    fun Base.hoo() {
        val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
    }

    open inner class Base(/*protected*/ val property: B) {
        fun baseFun() {}
    }

    fun caseForBase() {
        val base = Base(B())
        /*member of Base*/
        base.<!UNRESOLVED_REFERENCE!>baseFun<!>() //UNRESOLVED_REFERENCE
        base.<!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
        /*extensions*/
        base.hoo()
    }

    fun caseForChild() {
        val child = Child(B())
        /*member of Base*/
        child.<!UNRESOLVED_REFERENCE!>baseFun<!>() //UNRESOLVED_REFERENCE
        child.<!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
        /*member of Child*/
        child.<!UNRESOLVED_REFERENCE!>foo<!>() //UNRESOLVED_REFERENCE
        /*extensions*/
        child.hoo()
        child.voo()
    }
}


class Case2() {
    val x = object : Base(B()) {
        fun Base.zoo() {
            val x = property

        }

        fun foo() {
            baseFun()
            val x = property
            zoo()
            hoo()
        }
    }


    fun Base.hoo() {
        val x = property
    }

    open inner class Base(/*protected*/ val property: B) {
        fun baseFun() {}
    }

    fun caseForBase() {
        val base = Base(B())
        /*member of Base*/
        base.baseFun()
        base.property
        /*extensions*/
        base.hoo()
    }

    fun caseForChild() {
        val child = x
        /*member of Base*/
        child.baseFun()
        child.property
        /*extensions*/
        child.hoo()
    }
}


class Case3() {
    val x = object : A {
        inner class Child(property: B) : Base(property) {
            fun Base.zoo() {
                val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
            }

            fun foo() {
                <!UNRESOLVED_REFERENCE!>baseFun<!>() //UNRESOLVED_REFERENCE
                val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
                zoo()
                hoo()
            }
        }

        fun Child.voo() {
            val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
        }
        fun Base.hoo() {
            val x = <!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
        }

        open inner class Base(/*protected*/ val property: B) {
            fun baseFun() {}
        }

        fun caseForBase() {
            val base = Base(B())
            /*member of Base*/
            base.<!UNRESOLVED_REFERENCE!>baseFun<!>() //UNRESOLVED_REFERENCE
            base.<!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
            /*extensions*/
            base.hoo()
        }

        fun caseForChild() {
            val child = Child(B())
            /*member of Base*/
            child.<!UNRESOLVED_REFERENCE!>baseFun<!>() //UNRESOLVED_REFERENCE
            child.<!UNRESOLVED_REFERENCE!>property<!> //UNRESOLVED_REFERENCE
            /*member of Child*/
            child.<!UNRESOLVED_REFERENCE!>foo<!>() //UNRESOLVED_REFERENCE
            /*extensions*/
            child.hoo()
            child.voo()
        }
    }
}

interface A {}
class B() {}