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
    X[""]

    X.<!UNRESOLVED_REFERENCE!>set<!>("", "")
    X[""] = ""
    X[""] <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+=<!> ""

    X.<!UNRESOLVED_REFERENCE!>unaryPlus<!>()
    +X

    X.<!UNRESOLVED_REFERENCE!>not<!>()
    !X

    X.<!UNRESOLVED_REFERENCE!>plus<!>("")
    X + ""

    X.<!UNRESOLVED_REFERENCE!>rangeTo<!>("")
    X..""

    X.<!UNRESOLVED_REFERENCE!>contains<!>("")
    "" in X

    X.<!UNRESOLVED_REFERENCE!>invoke<!>()
    X()

    X.<!UNRESOLVED_REFERENCE!>plusAssign<!>("")
    X += ""
    <!NONE_APPLICABLE!>X<!NO_GET_METHOD!>[X]<!><!> <!UNRESOLVED_REFERENCE!>+=<!> ""

    // Must resolve to property
    Y = ""
    Y += ""

    X.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    X > ""

    X.<!UNRESOLVED_REFERENCE!>foo<!>("")
    X foo ""

    X.<!UNRESOLVED_REFERENCE!>interator<!>()
    for (x in X) {}

    // It seems it's okay to resolve to outer X because we assign `X` to delegate field where it's not used as receiver
    val delegated by X
}
