package kotlin

@Deprecated("")
@<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>DeprecatedSinceKotlin<!>("", errorSince = "1.0")
fun test1() {}

@Deprecated("")
@<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>DeprecatedSinceKotlin<!>("")
fun test2() {}

@Deprecated("")
@<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>DeprecatedSinceKotlin<!>("", "", "")
fun test3() {}

@Deprecated("")
@<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>DeprecatedSinceKotlin<!>("1.4-M2")
fun test4() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.3.70")
fun test5() {}
