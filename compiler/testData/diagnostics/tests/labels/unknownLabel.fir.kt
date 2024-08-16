// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-69829

inline fun Int.bar(f: (Int) -> Unit) {}

fun test1() {
    1.bar { if (it == 2) return<!UNRESOLVED_LABEL!>@foo<!> }
}

fun test2() {
    1.bar { 2.bar { if (it == 2) return<!UNRESOLVED_LABEL!>@foo<!> } }
}
