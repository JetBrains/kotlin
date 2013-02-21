// "Remove '*'" "true"
fun join(x : Int, vararg a : String) : String {
    val b = StringBuilder(x.toString())
    for (s in a) {
        b.append(s)
    }
    return b.toString()
}

fun main(args : Array<String>) {
    join(<caret>1, "2")
}