// ISSUE: KT-63068
fun List<Int>.f() {
    this<!UNRESOLVED_LABEL!>@List<!>.size
}

context(String)
fun Int.f() {
    this@String.length
    this<!UNRESOLVED_LABEL!>@Int<!>.toDouble()
}

context(String)
val p: String get() = this@String
