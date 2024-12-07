// RUN_PIPELINE_TILL: FRONTEND
enum class My { V }

fun test() {
    val ref = My::<!UNSUPPORTED!>V<!>
}
