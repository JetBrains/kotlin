// !WITH_NEW_INFERENCE

var longWords = 0
val smallWords = hashSetOf<String>()

fun test1(word: String) =
        run {
            if (word.length > 4) {
                <!OI;IMPLICIT_CAST_TO_ANY!>longWords++<!>
            }
            else {
                <!OI;IMPLICIT_CAST_TO_ANY!>smallWords.add(word)<!>
            }
        }

fun test2(word: String) =
        run {
            if (word.length > 4) {
                <!INVALID_IF_AS_EXPRESSION!>if<!> (word.startsWith("a")) <!OI;IMPLICIT_CAST_TO_ANY!>longWords++<!>
            }
            else {
                <!OI;IMPLICIT_CAST_TO_ANY!>smallWords.add(word)<!>
            }
        }

fun test3(word: String) =
        run {
            if (word.length > 4) {
                longWords++
            }
            else {
                System.out?.println(word) // Unit?
            }
        }