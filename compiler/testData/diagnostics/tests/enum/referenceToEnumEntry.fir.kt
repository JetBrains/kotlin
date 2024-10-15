// RUN_PIPELINE_TILL: SOURCE
enum class My { V }

fun test() {
    val ref = My::<!UNSUPPORTED!>V<!>
}
