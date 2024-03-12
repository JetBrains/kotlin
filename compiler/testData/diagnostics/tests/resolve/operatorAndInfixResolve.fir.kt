// ISSUE: KT-66453
// WITH_STDLIB
import kotlin.reflect.KProperty

class X(x: String) {
    companion object {
        operator fun get(x: String) {}
        operator fun get(x: X) = X
        operator fun set(x: String, v: String) {}
        operator fun unaryPlus() = this
        operator fun not() = this
        operator fun plus(x: String) {}
        operator fun rangeTo(x: String) {}
        operator fun contains(x: String) = true
        operator fun invoke() {}
        operator fun plusAssign(x: String) {}
        operator fun compareTo(x: String) = 0
        operator fun iterator() = iterator<String> {  }
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = ""

        infix fun foo(x: String) {}
    }
}

var Y = ""

fun test() {
    class X(y: Int)
    class Y

    X("")
    X(1)

    // All the following usages of X must resolve to the local class because it's used
    // as receiver.
    X.<!UNRESOLVED_REFERENCE!>get<!>("")
    X<!NO_GET_METHOD!>[""]<!>

    X.<!UNRESOLVED_REFERENCE!>set<!>("", "")
    X<!NO_SET_METHOD!>[""]<!> = ""
    X<!NO_GET_METHOD!>[""]<!> += ""

    X.<!UNRESOLVED_REFERENCE!>unaryPlus<!>()
    <!UNRESOLVED_REFERENCE!>+<!>X

    X.<!UNRESOLVED_REFERENCE!>not<!>()
    <!UNRESOLVED_REFERENCE!>!<!>X

    X.<!UNRESOLVED_REFERENCE!>plus<!>("")
    X <!UNRESOLVED_REFERENCE!>+<!> ""

    X.<!UNRESOLVED_REFERENCE!>rangeTo<!>("")
    X<!UNRESOLVED_REFERENCE!>..<!>""

    X.<!UNRESOLVED_REFERENCE!>contains<!>("")
    "" <!UNRESOLVED_REFERENCE!>in<!> X

    X.<!UNRESOLVED_REFERENCE!>invoke<!>()
    X()

    X.<!UNRESOLVED_REFERENCE!>plusAssign<!>("")
    X <!UNRESOLVED_REFERENCE!>+=<!> ""
    X<!NO_GET_METHOD!>[X]<!> += ""

    // Must resolve to property
    Y = ""
    Y += ""

    X.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    X <!UNRESOLVED_REFERENCE!>><!> ""

    X.<!UNRESOLVED_REFERENCE!>foo<!>("")
    X <!UNRESOLVED_REFERENCE!>foo<!> ""

    X.<!UNRESOLVED_REFERENCE!>interator<!>()
    for (x in <!ITERATOR_MISSING!>X<!>) {}

    // It seems it's okay to resolve to outer X because we assign `X` to delegate field where it's not used as receiver
    val delegated by X
}
