// ISSUE: KT-63068
fun List<Int>.f() {
    this<!UNRESOLVED_LABEL!>@List<!>.size
}

<!UNSUPPORTED_FEATURE!>context(String)<!>
fun Int.f() {
    this@String.length
    this<!UNRESOLVED_LABEL!>@Int<!>.toDouble()
}

<!UNSUPPORTED_FEATURE!>context(String)<!>
val p: String get() = this@String
