// CHECK_TYPE
/*
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-37120
 */

val case1 = object : A {
    inner class Child(property: B) : Base(property) {
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
    fun Child.voo() {
        val x = property
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
        val child = Child(B())
        /*member of Base*/
        child.baseFun()
        child.property
        /*member of Child*/
        child.foo()
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
                val x = property
            }

            fun foo() {
                baseFun()
                val x = property
                zoo()
                hoo()
            }
        }

        fun Child.voo() {
            val x = property
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
            val child = Child(B())
            /*member of Base*/
            child.baseFun()
            child.property
            /*member of Child*/
            child.foo()
            /*extensions*/
            child.hoo()
            child.voo()
        }
    }
}

interface A {}
class B() {}
