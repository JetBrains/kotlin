// "Remove useless '?'" "true"
fun f(a: Int) : Boolean {
    return a is Int?<caret>
}