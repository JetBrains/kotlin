@file:Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
package test

import kotlin.internal.RequireKotlin

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>)
fun f01() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"x"<!>)
fun f02() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1"<!>)
fun f03() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.0-beta"<!>)
fun f04() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.1.0-dev-1111"<!>)
fun f05() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.5.3.7"<!>)
fun f06() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1..0"<!>)
fun f07() {}

@RequireKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>" 1.0"<!>)
fun f08() {}


@RequireKotlin("1.1")
fun ok1() {}

@RequireKotlin("1.1.0")
fun ok2() {}

@RequireKotlin("0.0.0")
fun ok3() {}
