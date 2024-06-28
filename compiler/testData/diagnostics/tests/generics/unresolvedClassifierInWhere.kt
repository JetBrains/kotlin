// FIR_IDENTICAL
interface I0<T : <!UNRESOLVED_REFERENCE!>Unresolved0<!><String>>
interface I1<T> where T : <!UNRESOLVED_REFERENCE!>Unresolved1<!><String>
interface I2<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T : <!UNRESOLVED_REFERENCE!>Unresolved2<!><String><!>> where T : <!UNRESOLVED_REFERENCE!>Unresolved3<!><String>

fun <E : <!UNRESOLVED_REFERENCE!>Unresolved4<!><String>> foo0() {}
fun <E> foo1() where E : <!UNRESOLVED_REFERENCE!>Unresolved5<!><String> {}
fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>E : <!UNRESOLVED_REFERENCE!>Unresolved6<!><String><!>> foo2() where E : <!UNRESOLVED_REFERENCE!>Unresolved7<!><String> {}

val <E : <!UNRESOLVED_REFERENCE!>Unresolved7<!>> E.p1: Int
        get() = 1
val <E> E.p2: Int where E : <!UNRESOLVED_REFERENCE!>Unresolved8<!>
        get() = 1
val <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>E : <!UNRESOLVED_REFERENCE!>Unresolved9<!><!>> E.p3: Int where E : <!UNRESOLVED_REFERENCE!>Unresolved10<!>
        get() = 1
