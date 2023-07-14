// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR

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

    @JvmName("AB_protectedFinalFun")
    protected fun protectedFinalFun() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("AB_protectedOpenFun")<!>
    protected open fun protectedOpenFun() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("AB_protectedAbstractFun")<!>
    protected abstract fun protectedAbstractFun()

    @JvmName("AB_internalFinalFun")
    internal fun internalFinalFun() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("AB_internalOpenFun")<!>
    internal open fun internalOpenFun() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("AB_internalAbstractFun")<!>
    internal abstract fun internalAbstractFun()

    @JvmName("AB_privateFinalFun")
    private fun privateFinalFun() {}

    // Possible via AllOpen compiler plugin
    @JvmName("AB_privateOpenFun")
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun privateOpenFun() {}

    @JvmName("AB_privateAbstractFun")
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> fun privateAbstractFun()
}

class D: AB() {
    override fun absFun1() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("D_absFun2")<!>
    override fun absFun2() {}

    <!INAPPLICABLE_JVM_NAME!>@JvmName("D_openFun")<!>
    final override fun openFun() {}

    protected override fun protectedAbstractFun() {}

    internal override fun internalAbstractFun() {}

    @JvmName("D_finalFun")
    fun finalFun() {}
}

interface Intf {
    <!INAPPLICABLE_JVM_NAME!>@get:JvmName("getBar")<!> // no error in IDE
    <!INAPPLICABLE_JVM_NAME!>@set:JvmName("setBar")<!> // no error in IDE
    var foo: Int
}
