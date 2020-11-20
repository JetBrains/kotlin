// Issue: KT-41729

import kotlin.reflect.KProperty

class Foo {
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>) = 1
}

fun main(<!UNUSED_PARAMETER!>x<!>: Int) {
    val f = Foo()
    val a: Int
    <!UNRESOLVED_REFERENCE!>get<!>() = f.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>getValue<!>(null, ::<!UNSUPPORTED!>x<!>) // no exception after fix
    <!UNRESOLVED_REFERENCE!>print<!>(<!UNINITIALIZED_VARIABLE!>a<!>)
}