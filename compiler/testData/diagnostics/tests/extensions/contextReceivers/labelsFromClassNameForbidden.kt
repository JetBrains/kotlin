// ISSUE: KT-63068
fun List<Int>.f() {
    this<!UNRESOLVED_REFERENCE!>@List<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>size<!>
}

<!UNSUPPORTED_FEATURE!>context(String)<!>
fun Int.f() {
    this<!UNRESOLVED_REFERENCE!>@String<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>length<!>
    this<!UNRESOLVED_REFERENCE!>@Int<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>toDouble<!>()
}

<!UNSUPPORTED_FEATURE!>context(String)<!>
val p: String get() = this<!UNRESOLVED_REFERENCE!>@String<!>