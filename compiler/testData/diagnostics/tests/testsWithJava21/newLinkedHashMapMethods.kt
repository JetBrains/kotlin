// ISSUE: KT-64640
// WITH_STDLIB
// FIR_IDENTICAL

fun foo(x: LinkedHashMap<Int, String>, y: java.util.SequencedMap<Int, String>, z: HashMap<Int, String>) {
    x.putFirst(0, "0")
    x.putLast(1, "1")

    y.putFirst(0, "0")
    y.putLast(1, "1")

    z.<!UNRESOLVED_REFERENCE!>putFirst<!>(0, "0")
    z.<!UNRESOLVED_REFERENCE!>putLast<!>(1, "1")
}
