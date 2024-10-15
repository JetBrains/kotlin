// RUN_PIPELINE_TILL: SOURCE
fun String.f() {
    <!SUPER_NOT_AVAILABLE!>super@f<!>.compareTo("")
    <!SUPER_NOT_AVAILABLE!>super<!>.compareTo("")
}
