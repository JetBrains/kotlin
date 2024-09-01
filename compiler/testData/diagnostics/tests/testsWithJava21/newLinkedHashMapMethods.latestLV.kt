// LATEST_LV_DIFFERENCE
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-64640

fun foo(x: LinkedHashMap<Int, String>, y: java.util.SequencedMap<Int, String>, z: HashMap<Int, String>) {
    x.putFirst(0, "0")
    x.putLast(1, "1")
    x.putFirst(2, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.putLast(2, <!NULL_FOR_NONNULL_TYPE!>null<!>)

    y.putFirst(0, "0")
    y.putLast(1, "1")
    y.putFirst(2, null)
    y.putLast(3, null)

    z.<!UNRESOLVED_REFERENCE!>putFirst<!>(0, "0")
    z.<!UNRESOLVED_REFERENCE!>putLast<!>(1, "1")
}
