// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class E1

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class E2

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class E3

@E1
fun e1() {}

@E2
fun e2() {}

@E3
fun e3() {}

// FILE: usage.kt

package usage

import api.*

@OptIn(E1::class, E2::class, E3::class)
fun use1() {
    e1()
    e2()
    e3()
}

@OptIn(E1::class, E3::class)
fun use2() {
    e1()
    @OptIn(E2::class) e2()
    e3()
}

@OptIn(E1::class, E2::class)
fun use3() {
    e1()
    e2()
    <!EXPERIMENTAL_API_USAGE!>e3<!>()
}
