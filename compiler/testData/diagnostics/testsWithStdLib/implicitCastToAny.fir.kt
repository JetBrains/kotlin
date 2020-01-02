// !WITH_NEW_INFERENCE

var longWords = 0
val smallWords = hashSetOf<String>()

fun test1(word: String) =
        run {
            if (word.length > 4) {
                longWords++
            }
            else {
                smallWords.add(word)
            }
        }

fun test2(word: String) =
        run {
            if (word.length > 4) {
                if (word.startsWith("a")) longWords++
            }
            else {
                smallWords.add(word)
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