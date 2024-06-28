enum class My { V }

fun test() {
    val ref = My::<!UNRESOLVED_REFERENCE!>V<!>
}
