// !API_VERSION: 1.3

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.3")
class ClassCur

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.3")
fun funCur() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.3")
val valCur = Unit

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4")
class ClassNext

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4")
fun funNext() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4")
val valNext = Unit

fun usage() {
    <!DEPRECATION!>ClassCur<!>()
    <!DEPRECATION!>funCur<!>()
    <!DEPRECATION!>valCur<!>

    ClassNext()
    funNext()
    valNext
}
