// SKIP_TXT
fun bar(a: String): String {
    return <!RETURN_TYPE_MISMATCH!>when {
        a.length == 1 -> {
            when { // Error in K1, no error in K2
                a == "a" -> ""
                a == "b" -> ""
            }
        }

        else -> ""
    }<!>
}
