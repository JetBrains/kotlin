// DIAGNOSTICS: -JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE accidentally reported (K1 only) on x.addFirst/addLast/removeFirst/removeLast
// ISSUE: KT-68193
// ISSUE: KT-67804

fun <E1> addFirstLast(s: MutableList<String>, e: MutableList<E1>, sa: ArrayList<String>, ea: ArrayList<E1>, ev: E1) {
    s.addFirst(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    s.addFirst("")
    s.addLast(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    s.addLast("")

    e.addFirst(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    e.addFirst(ev)
    e.addLast(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    e.addLast(ev)

    sa.addFirst(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    sa.addFirst("")
    sa.addLast(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    sa.addLast("")

    ea.addFirst(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    ea.addFirst(ev)
    ea.addLast(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)
    ea.addLast(ev)
}

fun removeFirstLastString(s: MutableList<String>) {
    var x1 = s.removeFirst()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>
}

fun <E> removeFirstLastE(s: MutableList<E>) {
    var x1 = s.removeFirst()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>
}

fun removeFirstLastArrayListString(s: ArrayList<String>) {
    var x1 = s.removeFirst()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>
}

fun <E> removeFirstLastArrayListE(s: ArrayList<E>) {
    var x1 = s.removeFirst()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>

    var x2 = s.removeLast()
    x1 = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>
}
