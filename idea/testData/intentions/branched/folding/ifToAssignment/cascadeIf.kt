// WITH_RUNTIME
fun test(x: Any) {
    var res: String
    <caret>if (x is String)
        when {
            x.length > 3 -> res = "long string"
            else -> res = "short string"
        }
    else if (x is Int)
        when {
            x > 999 || x < -99 -> res = "long int"
            else -> res = "short int"
        }
    else if (x is Long)
        TODO()
    else
        res = "I don't know"
}