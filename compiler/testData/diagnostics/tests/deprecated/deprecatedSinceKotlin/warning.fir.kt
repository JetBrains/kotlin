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
    ClassCur()
    funCur()
    valCur

    ClassNext()
    funNext()
    valNext
}
