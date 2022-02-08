@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>)
fun f01() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"x"<!>)
fun f02() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1"<!>)
fun f03() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1,0"<!>)
fun f04() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1,0,1"<!>)
fun f05() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"a.b"<!>)
fun f06() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.a"<!>)
fun f07() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.0.a"<!>)
fun f08() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.0-beta"<!>)
fun f09() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.1.0-dev-1111"<!>)
fun f10() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.1.0+rc"<!>)
fun f11() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.5.3.7"<!>)
fun f12() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"01.1"<!>)
fun f13() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.01"<!>)
fun f14() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"-1.0"<!>)
fun f15() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.-1.0"<!>)
fun f16() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"0.00.1"<!>)
fun f17() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1..0"<!>)
fun f18() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>" 1.0"<!>)
fun f19() {}

@SinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.0 "<!>)
fun f20() {}




@SinceKotlin("1.1")
fun ok1() {}

@SinceKotlin("1.1.0")
fun ok2() {}

@SinceKotlin("0.0.0")
fun ok3() {}

@SinceKotlin(<!NEWER_VERSION_IN_SINCE_KOTLIN!>"123456789012345678901234567890.123456789012345678901234567890.123456789012345678901234567890"<!>)
fun ok4() {}
