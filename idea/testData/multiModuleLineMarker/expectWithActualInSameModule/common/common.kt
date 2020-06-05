package sample

expect fun <lineMarker descr="Has actuals in common">sameFile</lineMarker>()

actual fun <lineMarker>sameFile</lineMarker>() = Unit

expect fun <lineMarker descr="Has actuals in common">sameModule</lineMarker>()

fun noExpectActualDeclaration() = Unit