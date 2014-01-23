// OPTION: 1
fun foo(n: Int): Int {
    return try {
        n / 0
    } <caret>catch (e: ArithmeticException) {
        -1
    } catch (e: Exception) {
        -2
    } finally {

    }
}
