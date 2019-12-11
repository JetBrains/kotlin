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
    if (arg.first as? String != null) {
        // Should be still resolved to extension, without smart cast or smart cast impossible
        list.remove(arg.first)
    }
    val s = arg.first as? String
    if (s != null) {
        // Should be still resolved to extension, without smart cast or smart cast impossible
        list.remove(arg.first)
    }
}
