// ISSUE: KT-64982
// FIR_DUMP

class Outer<T> {
    companion object

    class Nested<S> {
        companion object
    }

    inner class Inner<R> {
        companion <!NESTED_CLASS_NOT_ALLOWED!>object<!>
    }

    object Obj
}

val test = <!FUNCTION_CALL_EXPECTED!>Outer<String><!>

val test2 = Outer.<!FUNCTION_CALL_EXPECTED!>Nested<String><!>

val test3 = <!FUNCTION_CALL_EXPECTED!>Outer<Int><!>.<!FUNCTION_CALL_EXPECTED!>Inner<Double><!>

val test4 = <!FUNCTION_CALL_EXPECTED!>Outer<Int><!>.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Obj<!>

val test5 = Outer

val test6 = Outer.Nested

val test7 = Outer.<!NO_COMPANION_OBJECT!>Inner<!>

val test8 = Outer.Obj
