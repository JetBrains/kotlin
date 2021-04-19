@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin

@RequireKotlin("")
fun f01() {}

@RequireKotlin("x")
fun f02() {}

@RequireKotlin("1")
fun f03() {}

@RequireKotlin("1.0-beta")
fun f04() {}

@RequireKotlin("1.1.0-dev-1111")
fun f05() {}

@RequireKotlin("1.5.3.7")
fun f06() {}

@RequireKotlin("1..0")
fun f07() {}

@RequireKotlin(" 1.0")
fun f08() {}


@RequireKotlin("1.1")
fun ok1() {}

@RequireKotlin("1.1.0")
fun ok2() {}

@RequireKotlin("0.0.0")
fun ok3() {}
