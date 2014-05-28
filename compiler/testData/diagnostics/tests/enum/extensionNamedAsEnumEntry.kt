enum class E {
    entry
}

val Int.entry: Int get() = 42
val Long.entry: Int get() = 239

val e = E.entry
