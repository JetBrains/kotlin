<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("")<!>
fun f01() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("x")<!>
fun f02() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1")<!>
fun f03() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1,0")<!>
fun f04() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1,0,1")<!>
fun f05() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("a.b")<!>
fun f06() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.a")<!>
fun f07() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.0.a")<!>
fun f08() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.0-beta")<!>
fun f09() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.1.0-dev-1111")<!>
fun f10() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.1.0+rc")<!>
fun f11() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.5.3.7")<!>
fun f12() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("01.1")<!>
fun f13() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.01")<!>
fun f14() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("-1.0")<!>
fun f15() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.-1.0")<!>
fun f16() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("0.00.1")<!>
fun f17() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1..0")<!>
fun f18() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin(" 1.0")<!>
fun f19() {}

<!ILLEGAL_SINCE_KOTLIN_VALUE!>@SinceKotlin("1.0 ")<!>
fun f20() {}




@SinceKotlin("1.1")
fun ok1() {}

@SinceKotlin("1.1.0")
fun ok2() {}

@SinceKotlin("0.0.0")
fun ok3() {}

@SinceKotlin("123456789012345678901234567890.123456789012345678901234567890.123456789012345678901234567890")
fun ok4() {}
