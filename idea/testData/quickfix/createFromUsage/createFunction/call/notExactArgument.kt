// "Create function 'synchronized'" "false"
// ACTION: Convert assignment to assignment expression
// ACTION: Convert expression to 'Int'
// ACTION: Round using roundToInt()
// ERROR: Type mismatch: inferred type is Float but Int was expected
// WITH_RUNTIME

fun test() {
    var value = 0
    synchronized(value) {
        value = <caret>10 / 1f
    }
}