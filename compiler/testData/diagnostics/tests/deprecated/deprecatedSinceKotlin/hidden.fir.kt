// !API_VERSION: 1.4

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
    <!INVISIBLE_REFERENCE!>funCur<!>()
    <!INVISIBLE_REFERENCE!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
