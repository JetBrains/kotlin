// "Migrate lambda syntax" "true"


val a = { <caret>(p: Int): String ->
    val v = p + 1
    v.toString()
    // returns v.toString()
}