// ISSUE: KT-65319

fun noInline(x: () -> Unit) {
    x.hashCode()
}

inline fun bar(s: () -> Unit) {
    noInline(<!REDUNDANT_LABEL_WARNING!>l1@<!> s)
}

fun main() {
    bar { }
}
