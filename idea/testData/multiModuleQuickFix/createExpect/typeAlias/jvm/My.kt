// "Create expected class in common module testModule_Common" "false"
// DISABLE-ERRORS
// ACTION: Remove 'actual' modifier

class Some(val x: Int, val y: Int) {
    fun processIt(z: Int) = x + y - z
}

actual typealias <caret>My = Some