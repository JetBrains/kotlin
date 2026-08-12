// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
fun test() {
    <!RUN_CALL_USED_TO_BREAK!>run {
        <!RUN_BROKEN_FOR_EACH_LIKE_CALL!>listOf("a", "b").forEach {
            val result = <!RUN_BROKEN_FOR_EACH_LIKE_CALL!>setOf(1, 2, 3).takeWhile { elem ->
                if (elem > 2) <!RUN_RETURN_USED_AS_BREAK!>return@run<!>
                <!RUN_RETURN_USED_AS_BREAK!>true<!>
            }<!>
            if (1 in result) <!RUN_RETURN_USED_AS_BREAK!>return@run<!>
        }<!>
    }<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, stringLiteral */
