// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

@jvmName("a")
fun foo() {}

@jvmName("b")
fun Any.foo() {}

<!WRONG_ANNOTATION_TARGET!>@jvmName("c")<!>
val px = 1

<!WRONG_ANNOTATION_TARGET!>@jvmName("d")<!>
val Any.px : Int
    get() = 1

val valx: Int
    @jvmName("e")
    get() = 1

var varx: Int
    @jvmName("f")
    get() = 1
    @jvmName("g")
    set(v) {}

var vardef: Int = 1
    @jvmName("h")
    get
    @jvmName("i")
    set

<!WRONG_ANNOTATION_TARGET!>@jvmName("C")<!>
class C <!WRONG_ANNOTATION_TARGET!>jvmName("primary")<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>jvmName("ctr")<!> constructor(x: Int): this() {}

    @jvmName("a")
    fun foo() {}

    @jvmName("b")
    fun Any.foo() {}

    <!WRONG_ANNOTATION_TARGET!>@jvmName("c")<!>
    val px = 1

    <!WRONG_ANNOTATION_TARGET!>@jvmName("d")<!>
    val Any.px : Int
        get() = 1

    val valx: Int
        @jvmName("e")
        get() = 1

    var varx: Int
        @jvmName("f")
        get() = 1
        @jvmName("g")
        set(v) {}
}

fun foo1() {
    <!INAPPLICABLE_JVM_NAME!>@jvmName("a")<!>
    fun foo() {}

    <!WRONG_ANNOTATION_TARGET!>@jvmName("a")<!>
    val x = 1
}

abstract class AB {
    <!INAPPLICABLE_JVM_NAME!>@jvmName("AB_absFun1")<!>
    abstract fun absFun1()

    abstract fun absFun2()

    <!INAPPLICABLE_JVM_NAME!>@jvmName("AB_openFun")<!>
    open fun openFun() {}
}

class D: AB() {
    override fun absFun1() {}

    <!INAPPLICABLE_JVM_NAME!>@jvmName("D_absFun2")<!>
    override fun absFun2() {}

    <!INAPPLICABLE_JVM_NAME!>@jvmName("D_openFun")<!>
    final override fun openFun() {}

    @jvmName("D_finalFun")
    fun finalFun() {}
}
