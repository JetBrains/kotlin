// !DIAGNOSTICS: -UNUSED_PARAMETER

class A

class D {
    val c: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>IncorrectThis<A>()<!>
}

val cTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>IncorrectThis<A>()<!>

class IncorrectThis<T> {
    fun get<R>(t: Any?, p: PropertyMetadata): Int {
        return 1
    }
}