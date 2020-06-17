// !API_VERSION: 1.3

@Deprecated("")
@DeprecatedSinceKotlin("", hiddenSince = "1.3")
class ClassCur

@Deprecated("")
@DeprecatedSinceKotlin("", hiddenSince = "1.3")
fun funCur() {}

@Deprecated("")
@DeprecatedSinceKotlin("", hiddenSince = "1.3")
val valCur = Unit

@Deprecated("")
@DeprecatedSinceKotlin("", hiddenSince = "1.4")
class ClassNext

@Deprecated("")
@DeprecatedSinceKotlin("", hiddenSince = "1.4")
fun funNext() {}

@Deprecated("")
@DeprecatedSinceKotlin("", hiddenSince = "1.4")
val valNext = Unit

fun usage() {
    ClassCur()
    funCur()
    valCur

    ClassNext()
    funNext()
    valNext
}
