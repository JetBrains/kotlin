// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

import kotlin.platform.*

@platformName("a")
fun foo() {}

@platformName("b")
fun Any.foo() {}

<!WRONG_ANNOTATION_TARGET!>@platformName("c")<!>
val px = 1

<!WRONG_ANNOTATION_TARGET!>@platformName("d")<!>
val Any.px : Int
    get() = 1

val valx: Int
    @platformName("e")
    get() = 1

var varx: Int
    @platformName("f")
    get() = 1
    @platformName("g")
    set(v) {}

var vardef: Int = 1
    @platformName("h")
    get
    @platformName("i")
    set

<!WRONG_ANNOTATION_TARGET!>@platformName("C")<!>
class C <!WRONG_ANNOTATION_TARGET!>platformName("primary")<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>platformName("ctr")<!> constructor(x: Int): this() {}

    @platformName("a")
    fun foo() {}

    @platformName("b")
    fun Any.foo() {}

    <!WRONG_ANNOTATION_TARGET!>@platformName("c")<!>
    val px = 1

    <!WRONG_ANNOTATION_TARGET!>@platformName("d")<!>
    val Any.px : Int
        get() = 1

    val valx: Int
        @platformName("e")
        get() = 1

    var varx: Int
        @platformName("f")
        get() = 1
        @platformName("g")
        set(v) {}
}

fun foo1() {
    <!INAPPLICABLE_PLATFORM_NAME!>@platformName("a")<!>
    fun foo() {}

    <!WRONG_ANNOTATION_TARGET!>@platformName("a")<!>
    val x = 1
}

abstract class AB {
    <!INAPPLICABLE_PLATFORM_NAME!>@platformName("AB_absFun1")<!>
    abstract fun absFun1()

    abstract fun absFun2()

    <!INAPPLICABLE_PLATFORM_NAME!>@platformName("AB_openFun")<!>
    open fun openFun() {}
}

class D: AB() {
    override fun absFun1() {}

    <!INAPPLICABLE_PLATFORM_NAME!>@platformName("D_absFun2")<!>
    override fun absFun2() {}

    <!INAPPLICABLE_PLATFORM_NAME!>@platformName("D_openFun")<!>
    final override fun openFun() {}

    @platformName("D_finalFun")
    fun finalFun() {}
}
