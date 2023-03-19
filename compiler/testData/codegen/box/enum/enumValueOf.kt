enum class E { OK }

fun <T> id(x: T) = x

fun box() = enumValueOf<E>(id("OK")).name
