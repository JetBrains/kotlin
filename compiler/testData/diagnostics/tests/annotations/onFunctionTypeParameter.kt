// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

annotation class Ann

fun f(@Ann x: Int) {}

val inVal: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}

fun inParam(fn: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (<!UNSUPPORTED!>@Ann<!> n: Int)->Unit)->Unit) {}

fun inReturn(): (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}

class A : (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}
    }

    val prop: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit
        get(): (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}
}

val inExtensionType: (<!UNSUPPORTED!>@Extension<!> Function1<*, *>).(<!UNSUPPORTED!>@Extension<!> Function1<*, *>)->Unit = {}

fun (@Extension Function1<*, *>).extFun(ff: @Extension Function1<*, *>) {}