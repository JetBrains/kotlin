// HIGHLIGHT: INFO

fun test() {
    var res: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        throw e
    }
}