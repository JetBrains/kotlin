import java.util.LinkedList

class A<T> : ArrayList<T>() {
    fun getFirst(): T = super.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    fun getLast(): T = super.<!UNRESOLVED_REFERENCE!>getLast<!>()
}

fun foo(x: List<String>, y: LinkedList<String>, z: A<String>) {
    x.<!UNRESOLVED_REFERENCE!>getFirst<!>()
    x.<!FUNCTION_CALL_EXPECTED!>first<!>
    x.first() // stdlib extension on List
    x.<!UNRESOLVED_REFERENCE!>getLast<!>()
    x.<!FUNCTION_CALL_EXPECTED!>last<!>
    x.last()

    y.<!DEPRECATION!>getFirst<!>()
    y.<!DEPRECATION!>first<!>
    y.first()
    y.<!DEPRECATION!>getLast<!>()
    y.<!DEPRECATION!>last<!>
    y.last()

    z.<!DEPRECATION!>getFirst<!>()
    z.<!FUNCTION_CALL_EXPECTED!>first<!>
    z.first()
    z.<!DEPRECATION!>getLast<!>()
    z.<!FUNCTION_CALL_EXPECTED!>last<!>
    z.last()
}