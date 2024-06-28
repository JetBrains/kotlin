// ISSUE: KT-65319

fun noInline(x: () -> Unit) {
    x.hashCode()
}

inline fun bar(s: () -> Unit) {
    noInline(l1@ <!USAGE_IS_NOT_INLINABLE!>s<!>)
}

fun main() {
    bar { }
}
