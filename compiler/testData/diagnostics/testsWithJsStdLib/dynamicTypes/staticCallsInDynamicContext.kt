// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun dynamic.test() {
    <!DEBUG_INFO_DYNAMIC!>foo<!>()
    <!DEBUG_INFO_DYNAMIC!>ext<!>()

    bar()
    this.<!DEBUG_INFO_DYNAMIC!>bar<!>()

    baz = 2
    this.<!DEBUG_INFO_DYNAMIC!>baz<!> = 2

    "".ext()
    <!DEBUG_INFO_DYNAMIC!>ext<!>()

    "".extValFun()
    <!DEBUG_INFO_DYNAMIC!>extValFun<!>()

    "".extVal
    <!DEBUG_INFO_DYNAMIC!>extVal<!>

    baz.extExtVal()
    <!DEBUG_INFO_DYNAMIC!>extExtVal<!>()

    ""()
    <!DEBUG_INFO_DYNAMIC!>this<!>()

    C() + C()
    <!NO_VALUE_FOR_PARAMETER!>+<!>C()

    this <!DEBUG_INFO_DYNAMIC!>+<!> C()

    0.<!UNRESOLVED_REFERENCE!>missing<!>()
}

fun bar() {}
var baz = 1

fun Any.ext() {}

val Any.extValFun: () -> Unit get() = null!!
val Any.extVal: () -> Unit get() = null!!

val Any.extExtVal: Any.() -> Unit get() = null!!

fun Any.invoke() {}

fun Any.plus(a: Any) {}

class C {

    fun String.invoke() {}
    val foo: String.() -> Unit = null!!

    val s: String = ""

    val withInvoke = WithInvoke()

    fun dynamic.test() {
        s()
        <!DEBUG_INFO_DYNAMIC!>this<!>()

        s.foo()
        this.<!DEBUG_INFO_DYNAMIC!>foo<!>()

        withInvoke()
        this@C.withInvoke()
    }
}

class WithInvoke {
    fun invoke() {}
}