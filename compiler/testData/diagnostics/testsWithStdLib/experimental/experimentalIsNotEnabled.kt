// FIR_IDENTICAL
// FILE: api.kt

@<!OPT_IN_IS_NOT_ENABLED!>RequiresOptIn<!>
@Retention(AnnotationRetention.BINARY)
annotation class Marker

@Marker
fun f() {}

// FILE: usage.kt

fun use1() {
    <!OPT_IN_USAGE_ERROR!>f<!>()
}

@Marker
fun use2() {
    f()
}

@<!OPT_IN_IS_NOT_ENABLED!>OptIn<!>(Marker::class)
fun use3() {
    f()
}
