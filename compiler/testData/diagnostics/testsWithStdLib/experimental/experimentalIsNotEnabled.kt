// FIR_IDENTICAL
// FILE: api.kt

@RequiresOptIn
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

@OptIn(Marker::class)
fun use3() {
    f()
}
