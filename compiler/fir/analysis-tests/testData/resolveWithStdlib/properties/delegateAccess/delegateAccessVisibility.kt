import kotlin.reflect.KProperty

val <T> T.delegate get() = this

class MyDelegate {
    var text = "Test"
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = text
}

val a by MyDelegate()
val b by private MyDelegate()
val c by <!WRONG_MODIFIER_TARGET!>protected<!> MyDelegate()
val d by internal MyDelegate()
val e by <!WRONG_MODIFIER_TARGET!>public<!> MyDelegate()

fun test() {
    val a2 = a#delegate
    val b2 = a#delegate
    val c2 = a#delegate
    val d2 = a#delegate
    val e2 = a#delegate

    val a3 by MyDelegate()
    val b3 by <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> MyDelegate()
    val c3 by <!WRONG_MODIFIER_TARGET!>protected<!> MyDelegate()
    val d3 by <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> MyDelegate()
    val e3 by <!WRONG_MODIFIER_TARGET!>public<!> MyDelegate()
}
