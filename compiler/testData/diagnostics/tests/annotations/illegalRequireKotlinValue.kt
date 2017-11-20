@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package test

import kotlin.internal.RequireKotlin

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("")<!>
fun f01() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("x")<!>
fun f02() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("1")<!>
fun f03() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("1.0-beta")<!>
fun f04() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("1.1.0-dev-1111")<!>
fun f05() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("1.5.3.7")<!>
fun f06() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin("1..0")<!>
fun f07() {}

<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>@RequireKotlin(" 1.0")<!>
fun f08() {}


@RequireKotlin("1.1")
fun ok1() {}

@RequireKotlin("1.1.0")
fun ok2() {}

@RequireKotlin("0.0.0")
fun ok3() {}
