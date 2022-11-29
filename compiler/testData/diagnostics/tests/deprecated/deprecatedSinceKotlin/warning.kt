// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// !API_VERSION: 1.4

package kotlin

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4")
class ClassCur

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4")
fun funCur() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4")
val valCur = Unit

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.5")
class ClassNext

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.5")
fun funNext() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.5")
val valNext = Unit

fun usage() {
    <!DEPRECATION!>ClassCur<!>()
    <!DEPRECATION!>funCur<!>()
    <!DEPRECATION!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
