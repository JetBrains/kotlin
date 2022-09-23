// FIR_IDENTICAL
// !API_VERSION: 1.4
// ALLOW_KOTLIN_PACKAGE


package kotlin

@Deprecated("")
@DeprecatedSinceKotlin(errorSince = "1.4")
class ClassCur

@Deprecated("")
@DeprecatedSinceKotlin(errorSince = "1.4")
fun funCur() {}

@Deprecated("")
@DeprecatedSinceKotlin(errorSince = "1.4")
val valCur = Unit

@Deprecated("")
@DeprecatedSinceKotlin(errorSince = "1.5")
class ClassNext

@Deprecated("")
@DeprecatedSinceKotlin(errorSince = "1.5")
fun funNext() {}

@Deprecated("")
@DeprecatedSinceKotlin(errorSince = "1.5")
val valNext = Unit

fun usage() {
    <!DEPRECATION_ERROR!>ClassCur<!>()
    <!DEPRECATION_ERROR!>funCur<!>()
    <!DEPRECATION_ERROR!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
