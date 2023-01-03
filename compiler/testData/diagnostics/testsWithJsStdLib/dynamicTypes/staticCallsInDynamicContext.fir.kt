// !MARK_DYNAMIC_CALLS
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun dynamic.test() {
    foo()
    ext()

    bar()
    this.bar()

    baz = 2
    this.baz = 2

    "".ext()
    ext()

    "".extValFun()
    extValFun()

    "".extVal
    extVal

    baz.extExtVal()
    extExtVal()

    ""()
    this()

    C() + C()
    <!UNRESOLVED_REFERENCE!>+<!>C()

    this + C()

    0.<!UNRESOLVED_REFERENCE!>missing<!>()
}

fun bar() {}
var baz = 1

fun Any.ext() {}

val Any.extValFun: () -> Unit get() = null!!
val Any.extVal: () -> Unit get() = null!!

val Any.extExtVal: Any.() -> Unit get() = null!!

operator fun Any.invoke() {}

operator fun Any.plus(a: Any) {}

class C {

    operator fun String.invoke() {}
    val foo: String.() -> Unit = null!!

    val s: String = ""

    val withInvoke = WithInvoke()

    fun dynamic.test() {
        s()
        this()

        s.foo()
        this.foo()

        withInvoke()
        this@C.withInvoke()
    }
}

class WithInvoke {
    operator fun invoke() {}
}
