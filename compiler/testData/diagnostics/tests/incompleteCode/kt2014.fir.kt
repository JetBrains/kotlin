//KT-2014 Better diagnostic when using property syntax to call a method
package c

class Foo {
    fun prop() : Int = 1
    fun bar(i: Int) : Int = i

    val a : Int = 1
}

fun x(f : Foo) {
    f.<!FUNCTION_CALL_EXPECTED!>prop<!>
    f.<!FUNCTION_CALL_EXPECTED!>bar<!>

    f.<!UNRESOLVED_REFERENCE!>a<!>()
    <!UNRESOLVED_REFERENCE!>c<!>()
    <!INVISIBLE_REFERENCE!>R<!>()
}

object R {}
