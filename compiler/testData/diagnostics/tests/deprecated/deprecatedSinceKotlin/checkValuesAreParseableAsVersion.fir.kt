package kotlin

@Deprecated("")
@DeprecatedSinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>, errorSince = "1.0")
fun test1() {}

@Deprecated("")
@DeprecatedSinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>)
fun test2() {}

@Deprecated("")
@DeprecatedSinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>, <!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>, <!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>""<!>)
fun test3() {}

@Deprecated("")
@DeprecatedSinceKotlin(<!ILLEGAL_KOTLIN_VERSION_STRING_VALUE!>"1.4-M2"<!>)
fun test4() {}

@Deprecated("")
@DeprecatedSinceKotlin("1.3.70")
fun test5() {}
