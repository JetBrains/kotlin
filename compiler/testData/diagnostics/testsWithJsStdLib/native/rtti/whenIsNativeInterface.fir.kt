external interface I

external interface J

fun box(a: Any) = when (a) {
    is I -> 0
    !is J -> 1
    else -> 2
}
