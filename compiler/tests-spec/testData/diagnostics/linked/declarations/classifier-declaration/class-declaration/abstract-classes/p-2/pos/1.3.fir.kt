// IGNORE_REVERSED_RESOLVE
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
// NOTE: attempt to implement inner abstract class as anonymous class
open class MainCase1() {
    private val priv = "privet"

    val implVal = object : MainCase1.InnerAbstractBase() {
        override fun foo(s: String) {
            println("object {$s}")
        }
    }

    abstract inner class InnerAbstractBase() {
        protected abstract fun foo(s: String)
        fun boo() {
            foo(priv)
        }
    }
}

fun case1() {
    val main = MainCase1()
    main.implVal.boo()
}



// TESTCASE NUMBER: 2
//NOTE: attempt to implement inner abstract class in init block
fun case2() {
    val main = MainCase2()
    main.impl.boo()
}


open class MainCase2() {
    private val priv = "privet"

    abstract inner class InnerAbstractBase() {
        protected abstract fun foo(s: String)
        fun boo() {
            foo(priv)
        }
    }

    var impl: InnerAbstractBase

    init {
        impl = object : MainCase2.InnerAbstractBase() {
            override fun foo(s: String) {
            }
        }
    }
}



//TESTCASE NUMBER: 3
// NOTE: attempt to inherit inner abstract class as another inner class
fun case3() {
    val main = MainCase3()
    main.ImplInnerAbstractBase().boo()
}

open class MainCase3() {
    private val priv = "privet"

    abstract inner class InnerAbstractBase() {
        protected abstract fun foo(s: String)
        fun boo() {
            foo(priv)
        }
    }

    inner class ImplInnerAbstractBase : MainCase3.InnerAbstractBase() {
        override fun foo(s: String) {
            println("ImplInnerAbstractBase {$s}")
        }
    }
}



// TESTCASE NUMBER: 4
// NOTE: attempt to inherit inner abstract class in a outer class function
fun case4() {
    val main = MainCase4()
    main.zoo().boo()
}


open class MainCase4() {
    private val priv = "privet"

    abstract inner class InnerAbstractBase() {
        protected abstract fun foo(s: String)
        fun boo() {
            foo(priv)
        }
    }

    fun zoo(): MainCase4.InnerAbstractBase {
        class ImplInnerAbstractBaseZoo : MainCase4.InnerAbstractBase() {
            override fun foo(s: String) {
                println("zoo fun: {$s}")
            }
        }
        return ImplInnerAbstractBaseZoo()
    }
}
