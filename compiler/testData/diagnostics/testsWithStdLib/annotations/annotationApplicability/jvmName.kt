// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

@JvmName("a")
fun foo() {}

@JvmName("b")
fun Any.foo() {}

<!WRONG_ANNOTATION_TARGET!>@JvmName("c")<!>
val px = 1

<!WRONG_ANNOTATION_TARGET!>@JvmName("d")<!>
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

<!WRONG_ANNOTATION_TARGET!>@JvmName("C")<!>
class C <!WRONG_ANNOTATION_TARGET!>@JvmName("primary")<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>@JvmName("ctr")<!> constructor(x: Int): this() {}

    @JvmName("a")
    fun foo() {}

    @JvmName("b")
    fun Any.foo() {}

    <!WRONG_ANNOTATION_TARGET!>@JvmName("c")<!>
    val px = 1

    <!WRONG_ANNOTATION_TARGET!>@JvmName("d")<!>
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
    <!INAPPLICABLE_JVM_NAME!>@JvmName("a")<!>
    fun foo() {}

    <!WRONG_ANNOTATION_TARGET!>@JvmName("a")<!>
    val x = 1
}

abstract class AB {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("AB_absFun1")<!>
    abstract fun absFun1()

    abstract fun absFun2()

    <!INAPPLICABLE_JVM_NAME!>@JvmName("AB_openFun")<!>
    open fun openFun() {}
}

class D: AB() {
    override fun absFun1() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("D_absFun2")<!>
    override fun absFun2() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("D_openFun")<!>
    final override fun openFun() {}

    @JvmName("D_finalFun")
    fun finalFun() {}
}

interface Intf {
    <!INAPPLICABLE_JVM_NAME!>@get:JvmName("getBar")<!> // no error in IDE
    <!INAPPLICABLE_JVM_NAME!>@set:JvmName("setBar")<!> // no error in IDE
    var foo: Int
}