// FIR_IDENTICAL
// !API_VERSION: 1.4
// ALLOW_KOTLIN_PACKAGE

package kotlin

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "1.4")
class ClassCur

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "1.4")
fun funCur() {}

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "1.4")
val valCur = Unit

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "1.5")
class ClassNext

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "1.5")
fun funNext() {}

@Deprecated("")
@DeprecatedSinceKotlin(hiddenSince = "1.5")
val valNext = Unit

fun usage() {
    <!DEPRECATION_ERROR!>ClassCur<!>()
    <!UNRESOLVED_REFERENCE!>funCur<!>()
    <!UNRESOLVED_REFERENCE!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
