class A

class D {
    val c: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>IncorrectThis<A>()<!>
}

val cTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>IncorrectThis<A>()<!>

class IncorrectThis<T> {
    fun get<R>(t: Any?, p: String): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }
}