// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun f(x: Int = 0) {}

val inVal: (x: Int = <!UNSUPPORTED!>0<!>)->Unit = {}

fun inParam(fn: (x: Int = <!UNSUPPORTED!>0<!>)->Unit) {}

fun inParamNested(fn1: (fn2: (n: Int = <!UNSUPPORTED!>0<!>)->Unit)->Unit) {}

fun inReturn(): (x: Int = <!UNSUPPORTED!>0<!>)->Unit = {}

class A : (Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (x: Int = <!UNSUPPORTED!>0<!>)->Unit = {}
    }

    val prop: (x: Int = <!UNSUPPORTED!>0<!>)->Unit
        get(): (x: Int = <!UNSUPPORTED!>0<!>)->Unit = {}
}
