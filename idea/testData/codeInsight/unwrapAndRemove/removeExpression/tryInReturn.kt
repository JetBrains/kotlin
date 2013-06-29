// OPTION: 1
fun foo(n : Int): Int {
    return <caret>try {
        n/0
    } catch (e: Exception) {
        -1
    }
}
