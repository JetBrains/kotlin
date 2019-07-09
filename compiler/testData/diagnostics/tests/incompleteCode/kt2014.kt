//KT-2014 Better diagnostic when using property syntax to call a method
package c

class Foo {
    fun prop() : Int = 1
    fun bar(i: Int) : Int = i

    val a : Int = 1
}

fun x(f : Foo) {
    f.<!FUNCTION_CALL_EXPECTED!>prop<!>
    f.<!FUNCTION_CALL_EXPECTED, NO_VALUE_FOR_PARAMETER!>bar<!>

    f.<!FUNCTION_EXPECTED!>a<!>()
    <!UNRESOLVED_REFERENCE!>c<!>()
    <!FUNCTION_EXPECTED!>R<!>()
}

object R {}