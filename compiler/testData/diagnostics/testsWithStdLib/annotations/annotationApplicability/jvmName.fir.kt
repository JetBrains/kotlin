// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

@JvmName("a")
fun foo() {}

@JvmName("b")
fun Any.foo() {}

@JvmName("c")
val px = 1

@JvmName("d")
val Any.px : Int
    get() = 1

val valx: Int
    @JvmName("e")
    get() = 1

var varx: Int
    @JvmName("f")
    get() = 1
    @JvmName("g")
    set(v) {}

var vardef: Int = 1
    @JvmName("h")
    get
    @JvmName("i")
    set

@JvmName("C")
class C @JvmName("primary") constructor() {
    @JvmName("ctr") constructor(x: Int): this() {}

    @JvmName("a")
    fun foo() {}

    @JvmName("b")
    fun Any.foo() {}

    @JvmName("c")
    val px = 1

    @JvmName("d")
    val Any.px : Int
    get() = 1

    val valx: Int
    @JvmName("e")
    get() = 1

    var varx: Int
    @JvmName("f")
    get() = 1
    @JvmName("g")
    set(v) {}
}

fun foo1() {
    @JvmName("a")
    fun foo() {}

    @JvmName("a")
    val x = 1
}

abstract class AB {
    @JvmName("AB_absFun1")
    abstract fun absFun1()

    abstract fun absFun2()

    @JvmName("AB_openFun")
    open fun openFun() {}
}

class D: AB() {
    override fun absFun1() {}

    @JvmName("D_absFun2")
    override fun absFun2() {}

    @JvmName("D_openFun")
    final override fun openFun() {}

    @JvmName("D_finalFun")
    fun finalFun() {}
}

interface Intf {
    @get:JvmName("getBar") // no error in IDE
    @set:JvmName("setBar") // no error in IDE
    var foo: Int
}