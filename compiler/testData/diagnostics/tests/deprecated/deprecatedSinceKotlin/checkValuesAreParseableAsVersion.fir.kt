package kotlin

@Deprecated("")
@DeprecatedSinceKotlin("", errorSince = "1.0")
fun test1() {}

@Deprecated("")
@DeprecatedSinceKotlin("")
fun test2() {}

@Deprecated("")
@DeprecatedSinceKotlin("", "", "")
fun test3() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.4-M2")
fun test4() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.3.70")
fun test5() {}
