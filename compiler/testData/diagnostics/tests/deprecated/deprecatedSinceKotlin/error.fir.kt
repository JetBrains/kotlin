// !API_VERSION: 1.3

@DeprecatedSinceKotlin("", errorSince = "1.3")
class ClassCur

@DeprecatedSinceKotlin("", errorSince = "1.3")
fun funCur() {}

@DeprecatedSinceKotlin("", errorSince = "1.3")
val valCur = Unit

@DeprecatedSinceKotlin("", errorSince = "1.4")
class ClassNext

@DeprecatedSinceKotlin("", errorSince = "1.4")
fun funNext() {}

@DeprecatedSinceKotlin("", errorSince = "1.4")
val valNext = Unit

fun usage() {
    <!DEPRECATION_ERROR!>ClassCur<!>()
    <!DEPRECATION_ERROR!>funCur<!>()
    <!DEPRECATION_ERROR!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
