// Issue: KT-41729

import kotlin.reflect.KProperty

class Foo {
    operator fun <T> getValue(thisRef: Any?, property: KProperty<*>) = 1
}

fun main(x: Int) {
    val f = Foo()
    val a: Int
    <!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = f.getValue(null, ::<!UNSUPPORTED!>x<!>) // no exception after fix
    <!UNRESOLVED_REFERENCE!>print<!>(<!UNINITIALIZED_VARIABLE!>a<!>)
}
