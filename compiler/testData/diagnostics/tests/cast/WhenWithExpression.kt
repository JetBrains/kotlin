enum class List<out T>(val size : Int) {
    Nil : List<Nothing>(0)
}

fun List<String>.join() =
        when (this) {
            List.Nil -> "[]" // CANNOT_CHECK_FOR_ERASED was reported
            else -> ""
        }
