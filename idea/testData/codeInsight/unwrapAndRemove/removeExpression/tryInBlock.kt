// OPTION: 0
fun foo(n : Int): Int {
    <caret>try {
        n/0
    } catch (e: Exception) {
        -1
    }

    return 0
}
