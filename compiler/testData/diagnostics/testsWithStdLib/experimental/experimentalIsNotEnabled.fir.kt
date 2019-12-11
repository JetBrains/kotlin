// FILE: api.kt

@Experimental
annotation class Marker

@Marker
fun f() {}

// FILE: usage.kt

fun use1() {
    f()
}

@Marker
fun use2() {
    f()
}

@UseExperimental(Marker::class)
fun use3() {
    f()
}
