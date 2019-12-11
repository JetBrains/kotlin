interface Inter {
    fun foo(x: Int = <!UNRESOLVED_REFERENCE!>y<!>, y: Int = x)
}

abstract class Abst {
    abstract fun foo(x: Int = <!UNRESOLVED_REFERENCE!>y<!>, y: Int = x)
}

fun extraDiagnostics(x: Int = <!UNRESOLVED_REFERENCE!>y<!>, y: Int)
