// OPTION: 1
fun foo(n : Int): Int {
    <caret>try {
        val m = n + 1
        m/0
    } catch (e: Exception) {
        -1
    }

    return 0
}
