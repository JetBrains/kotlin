// !API_VERSION: 1.3

@Deprecated("")
@DeprecatedSinceKotlin("", errorSince = "1.3")
class ClassCur

@Deprecated("")
@DeprecatedSinceKotlin("", errorSince = "1.3")
fun funCur() {}

@Deprecated("")
@DeprecatedSinceKotlin("", errorSince = "1.3")
val valCur = Unit

@Deprecated("")
@DeprecatedSinceKotlin("", errorSince = "1.4")
class ClassNext

@Deprecated("")
@DeprecatedSinceKotlin("", errorSince = "1.4")
fun funNext() {}

@Deprecated("")
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
