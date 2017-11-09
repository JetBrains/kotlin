// WITH_RUNTIME
fun test() {
    var res: String? = null

    <caret>try {
        try {
            res = "success"
        } catch (e: Exception) {
            TODO()
        }
    } catch (e: Exception) {
        try {
            TODO()
        } catch (e: Exception) {
            res = "failure"
        }
    }
}