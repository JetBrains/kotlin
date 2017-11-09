fun test(n: Int) {
    var res: String? = null

    if (n == 1) {
        <caret>try {
            res = "success"
        } catch (e: Exception) {
            throw e
        }
    }
    else {
        res = "else"
    }
}
