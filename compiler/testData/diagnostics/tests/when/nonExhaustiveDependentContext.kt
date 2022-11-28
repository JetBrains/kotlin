// SKIP_TXT
fun bar(a: String): String {
    return when {
        a.length == 1 -> {
            <!NO_ELSE_IN_WHEN!>when<!> { // Error in K1, no error in K2
                a == "a" -> ""
                a == "b" -> ""
            }
        }

        else -> ""
    }
}
