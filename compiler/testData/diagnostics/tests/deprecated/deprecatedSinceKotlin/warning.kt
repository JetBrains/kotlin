// !API_VERSION: 1.3

@DeprecatedSinceKotlin("", warningSince = "1.3")
class ClassCur

@DeprecatedSinceKotlin("", warningSince = "1.3")
fun funCur() {}

@DeprecatedSinceKotlin("", warningSince = "1.3")
val valCur = Unit

@DeprecatedSinceKotlin("", warningSince = "1.4")
class ClassNext

@DeprecatedSinceKotlin("", warningSince = "1.4")
fun funNext() {}

@DeprecatedSinceKotlin("", warningSince = "1.4")
val valNext = Unit

fun usage() {
    <!DEPRECATION!>ClassCur<!>()
    <!DEPRECATION!>funCur<!>()
    <!DEPRECATION!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
