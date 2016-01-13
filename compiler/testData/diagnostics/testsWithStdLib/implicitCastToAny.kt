var longWords = 0
val smallWords = hashSetOf<String>()

fun test1(word: String) =
        run {
            if (word.length > 4) {
                <!IMPLICIT_CAST_TO_ANY!>longWords++<!>
            }
            else {
                <!IMPLICIT_CAST_TO_ANY!>smallWords.add(word)<!>
            }
        }

fun test2(word: String) =
        run {
            if (word.length > 4) {
                <!INVALID_IF_AS_EXPRESSION, IMPLICIT_CAST_TO_ANY!>if (word.startsWith("a")) longWords++<!>
            }
            else {
                <!IMPLICIT_CAST_TO_ANY!>smallWords.add(word)<!>
            }
        }