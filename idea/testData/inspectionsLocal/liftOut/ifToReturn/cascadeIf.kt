// WITH_RUNTIME
fun test(x: Any): String {
    <caret>if (x is String)
        when {
            x.length > 3 -> return "long string"
            else -> return "short string"
        }
    else if (x is Int)
        when {
            x > 999 || x < -99 -> return "long int"
            else -> return "short int"
        }
    else if (x is Long)
        TODO()
    else
        return "I don't know"
}