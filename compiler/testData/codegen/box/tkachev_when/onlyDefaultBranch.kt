// technically, this is exhaustive, so it is legal. Feels illegal, though

fun box(): String {
    return when {
        else -> "OK"
    }
}