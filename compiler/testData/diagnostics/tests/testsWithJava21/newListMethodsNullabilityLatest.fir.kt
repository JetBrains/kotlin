// DIAGNOSTICS: -JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE accidentally reported (K1 only) on x.addFirst/addLast/removeFirst/removeLast
// ISSUE: KT-68193
// ISSUE: KT-67804

fun <E1> addFirstLast(s: MutableList<String>, e: MutableList<E1>, sa: ArrayList<String>, ea: ArrayList<E1>, ev: E1) {
    s.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    s.addFirst("")
    s.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    s.addLast("")

    e.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    e.addFirst(ev)
    e.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    e.addLast(ev)

    sa.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    sa.addFirst("")
    sa.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    sa.addLast("")

    ea.addFirst(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    ea.addFirst(ev)
    ea.addLast(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    ea.addLast(ev)
}

fun removeFirstLastString(s: MutableList<String>) {
    var x1 = s.removeFirst()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

fun <E> removeFirstLastE(s: MutableList<E>) {
    var x1 = s.removeFirst()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

fun removeFirstLastArrayListString(s: ArrayList<String>) {
    var x1 = s.removeFirst()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

fun <E> removeFirstLastArrayListE(s: ArrayList<E>) {
    var x1 = s.removeFirst()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULL_FOR_NONNULL_TYPE!>null<!>
}
