interface Inter {
    fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int = x)
}

abstract class Abst {
    abstract fun foo(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int = x)
}

<!NON_MEMBER_FUNCTION_NO_BODY!>fun extraDiagnostics(<!UNUSED_PARAMETER!>x<!>: Int = <!UNINITIALIZED_PARAMETER!>y<!>, <!UNUSED_PARAMETER!>y<!>: Int)<!>