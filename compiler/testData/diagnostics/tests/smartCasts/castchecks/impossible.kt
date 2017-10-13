// !LANGUAGE: +SafeCastCheckBoundSmartCasts
// See KT-20752

class Unstable {
    val first: String? get() = null
}

class StringList {
    fun remove(s: String) = s
}

fun StringList.remove(s: String?) = s ?: ""

fun foo(list: StringList, arg: Unstable) {
    list.remove(arg.first)
    if (arg.first <!USELESS_CAST!>as? String<!> != null) {
        // Ideally should have smart cast impossible here
        list.remove(arg.first)
    }
    val s = arg.first <!USELESS_CAST!>as? String<!>
    if (s != null) {
        // Ideally should have smart cast impossible here
        list.remove(arg.first)
    }
}
