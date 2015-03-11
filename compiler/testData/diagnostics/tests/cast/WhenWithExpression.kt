class List<out T>(val size : Int) {
    default object {
        val Nil = List<Nothing>(0)
    }
}

fun List<String>.join() =
        when (this) {
            List.Nil -> "[]" // CANNOT_CHECK_FOR_ERASED was reported
            else -> ""
        }
