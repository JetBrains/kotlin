// WITH_STDLIB

var sideEffect = "Fail"

object Property {
    init {
        sideEffect = "OK"
    }

    const val PROPERTY_VALUE: String = "O"
}

const val TOP_LEVEL_PROPERTY_VALUE: String = "K"

val value1: String by Property::PROPERTY_VALUE
val value2: String by ::TOP_LEVEL_PROPERTY_VALUE

fun box(): String {
    if (sideEffect != "OK") return "Side effect wasn't executed"
    return value1 + value2
}
