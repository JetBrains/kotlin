// FIR_IDENTICAL
// LANGUAGE_VERSION: 2.0
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// DIAGNOSTICS: -JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE accidentally reported (K1 only) on x.addFirst/addLast/removeFirst/removeLast
// ISSUE: KT-68193
// ISSUE: KT-67804

fun <E1> addFirstLast(s: MutableList<String>, e: MutableList<E1>, sa: ArrayList<String>, ea: ArrayList<E1>, ev: E1) {
    s.addFirst(null)
    s.addFirst("")
    s.addLast(null)
    s.addLast("")

    e.addFirst(null)
    e.addFirst(ev)
    e.addLast(null)
    e.addLast(ev)

    sa.addFirst(null)
    sa.addFirst("")
    sa.addLast(null)
    sa.addLast("")

    ea.addFirst(null)
    ea.addFirst(ev)
    ea.addLast(null)
    ea.addLast(ev)
}

fun removeFirstLastString(s: MutableList<String>) {
    var x1 = s.removeFirst()
    x1 = null

    var x2 = s.removeLast()
    x1 = null
}

fun <E> removeFirstLastE(s: MutableList<E>) {
    var x1 = s.removeFirst()
    x1 = null

    var x2 = s.removeLast()
    x1 = null
}

fun removeFirstLastArrayListString(s: ArrayList<String>) {
    var x1 = s.removeFirst()
    x1 = null

    var x2 = s.removeLast()
    x1 = null
}

fun <E> removeFirstLastArrayListE(s: ArrayList<E>) {
    var x1 = s.removeFirst()
    x1 = null

    var x2 = s.removeLast()
    x1 = null
}
