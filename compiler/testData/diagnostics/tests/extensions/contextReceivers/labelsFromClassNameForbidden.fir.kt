fun List<Int>.f() {
    this<!UNRESOLVED_LABEL!>@List<!>.size
}

context(String)
fun Int.f() {
    this<!UNRESOLVED_LABEL!>@String<!>.length
    this<!UNRESOLVED_LABEL!>@Int<!>.toDouble()
}

context(String)
val p: String get() = this<!UNRESOLVED_LABEL!>@String<!>