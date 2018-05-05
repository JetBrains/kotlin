// FILE: api.kt

@<!EXPERIMENTAL_IS_NOT_ENABLED!>Experimental<!>
annotation class Marker

@Marker
fun f() {}

// FILE: usage.kt

fun use1() {
    <!EXPERIMENTAL_API_USAGE_ERROR!>f<!>()
}

@Marker
fun use2() {
    f()
}

@<!EXPERIMENTAL_IS_NOT_ENABLED!>UseExperimental<!>(Marker::class)
fun use3() {
    f()
}
