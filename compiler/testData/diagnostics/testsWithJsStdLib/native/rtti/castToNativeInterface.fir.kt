external interface I

fun box(a: Any, b: Any): Pair<I, I?> {
    return Pair(a as I, b as? I)
}
