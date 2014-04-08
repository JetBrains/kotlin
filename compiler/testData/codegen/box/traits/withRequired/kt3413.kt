trait Trait<T : Enum<T>> : Enum<T> {
    val value : String get() = name()
}

enum class E : Trait<E> {
    OK
}

fun box() = E.OK.value
