// !API_VERSION: 1.3

@DeprecatedSinceKotlin("", hiddenSince = "1.3")
class ClassCur

@DeprecatedSinceKotlin("", hiddenSince = "1.3")
fun funCur() {}

@DeprecatedSinceKotlin("", hiddenSince = "1.3")
val valCur = Unit

@DeprecatedSinceKotlin("", hiddenSince = "1.4")
class ClassNext

@DeprecatedSinceKotlin("", hiddenSince = "1.4")
fun funNext() {}

@DeprecatedSinceKotlin("", hiddenSince = "1.4")
val valNext = Unit

fun usage() {
    <!DEPRECATION_ERROR!>ClassCur<!>()
    <!UNRESOLVED_REFERENCE!>funCur<!>()
    <!UNRESOLVED_REFERENCE!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
