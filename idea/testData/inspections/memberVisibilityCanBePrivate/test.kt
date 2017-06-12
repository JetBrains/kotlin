val a = "a"

fun f1() {}

fun f2() {
    println(a)
    f1()
    A().b
}

class A {
    val unused = ""
    val a = ""
    internal val b = ""
    protected val c = ""
    private val d = ""

    fun unused() {}
    fun f1() {}
    internal fun f2() {}
    protected fun f3() {}
    private fun f4() {}

    fun bar() {
        println(a)
        println(b)
        println(c)
        println(d)
        f1()
        f2()
        f3()
        f4()
    }
}

interface I {
    val x: String
    fun foo()
}

class B : I {
    override val x: String
        get() = ""

    override fun foo() {
    }

    fun bar() {
        println(x)
        foo()
    }
}

interface I2 {
    val x: String
    fun foo()
    fun bar() {
        println(x)
        foo()
    }
}

open class C {
    open val x = ""
    protected val y = ""

    open fun f1() {}
    protected fun f2() {}

    fun bar() {
        println(x)
        println(y)
        f1()
        f2()
    }
}

class D(val a: String = "",
        var b: String = "",
        internal val c: String = "",
        protected val d: String = "",
        private val e: String = "") {
    fun foo() {
        println(a)
        println(b)
        println(c)
        println(d)
        println(e)
    }
}

open class E(override val x: String = "",
             open val a: String = "") : I {
    override fun foo() {}
    fun foo() {
        println(x)
        println(a)
    }

    fun bar() {
        var v1 = ""
        val v2 = ""
        println(v1)
        println(v2)
    }
}

val x = object {
    val a: String = "",
    internal val b: String = "",
    protected val c: String = "",
    private val d: String = ""

    fun f1() {}
    internal fun f2() {}
    protected fun f3() {}
    private fun f4() {}

    fun foo() {
        println(a)
        println(b)
        println(c)
        println(d)
        f1()
        f2()
        f3()
        f4()
    }
}