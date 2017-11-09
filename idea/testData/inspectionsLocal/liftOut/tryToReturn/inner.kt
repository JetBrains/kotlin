fun test(n: Int): String {
    if (n == 1) {
        <caret>try {
            return "success"
        } catch (e: Exception) {
            throw e
        }
    }
    else {
        return "else"
    }
}
