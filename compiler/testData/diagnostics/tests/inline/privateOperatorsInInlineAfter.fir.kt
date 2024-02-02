// WITH_STDLIB
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +ProhibitPrivateOperatorCallInInline

import kotlin.reflect.KProperty
import kotlin.properties.ReadOnlyProperty

private operator fun String.getValue(nothing: Any?, property: KProperty<*>) = "OK"
private operator fun String.setValue(nothing: Any?, property: KProperty<*>, s: String) {}
private operator fun Int.provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, Int> = ReadOnlyProperty { _, _ -> this }

private operator fun O.compareTo(other: O) = 1
private operator fun O.contains(other: O) = true
private operator fun O.invoke() {}
private operator fun O.iterator() = O
private operator fun O.next() = O
private operator fun O.hasNext() = false
private operator fun O.get(i: Int) = O
private operator fun O.set(i: Int, o: O) {}
private operator fun O.component1() = O
private operator fun O.inc() = O
private operator fun O.not() = O
private operator fun O.plus(other: O) = O
private operator fun O.unaryPlus() = O
private operator fun O.rangeTo(other: O) = O
private operator fun O.rangeUntil(other: O) = O
private operator fun O.plusAssign(other: O) {}

object O
object P

operator fun P.iterator() = P
private operator fun P.next() = P
private operator fun P.hasNext() = false

inline fun foo() {
    var x by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>""<!>
    x
    x = ""

    val y by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>1<!>
    y

    var o = O
    o <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!><<!> o
    o <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>in<!> o
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>o<!>()
    for (o1 in <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>o<!>) {
    }
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>o[1]<!>
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>o[1]<!> = o
    val (<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>o2<!>) = o
    o<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>++<!>
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>!<!>o
    o <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>+<!> o
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>+<!>o
    o<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>..<!>o
    o<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>..<<!>o
    O <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>+=<!> o

    for (p in <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>P<!>) {
    }
}
