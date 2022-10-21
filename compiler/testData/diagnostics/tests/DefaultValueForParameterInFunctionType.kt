// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class A : (Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (x: Int = <!UNSUPPORTED!>0<!>)->Unit = {}
    }

    val prop: (x: Int = <!UNSUPPORTED!>0<!>)->Unit
        get(): (x: Int = <!UNSUPPORTED!>0<!>)->Unit = {}
}
