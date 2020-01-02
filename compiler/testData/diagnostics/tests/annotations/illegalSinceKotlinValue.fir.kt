@SinceKotlin("")
fun f01() {}

@SinceKotlin("x")
fun f02() {}

@SinceKotlin("1")
fun f03() {}

@SinceKotlin("1,0")
fun f04() {}

@SinceKotlin("1,0,1")
fun f05() {}

@SinceKotlin("a.b")
fun f06() {}

@SinceKotlin("1.a")
fun f07() {}

@SinceKotlin("1.0.a")
fun f08() {}

@SinceKotlin("1.0-beta")
fun f09() {}

@SinceKotlin("1.1.0-dev-1111")
fun f10() {}

@SinceKotlin("1.1.0+rc")
fun f11() {}

@SinceKotlin("1.5.3.7")
fun f12() {}

@SinceKotlin("01.1")
fun f13() {}

@SinceKotlin("1.01")
fun f14() {}

@SinceKotlin("-1.0")
fun f15() {}

@SinceKotlin("1.-1.0")
fun f16() {}

@SinceKotlin("0.00.1")
fun f17() {}

@SinceKotlin("1..0")
fun f18() {}

@SinceKotlin(" 1.0")
fun f19() {}

@SinceKotlin("1.0 ")
fun f20() {}




@SinceKotlin("1.1")
fun ok1() {}

@SinceKotlin("1.1.0")
fun ok2() {}

@SinceKotlin("0.0.0")
fun ok3() {}

@SinceKotlin("123456789012345678901234567890.123456789012345678901234567890.123456789012345678901234567890")
fun ok4() {}
