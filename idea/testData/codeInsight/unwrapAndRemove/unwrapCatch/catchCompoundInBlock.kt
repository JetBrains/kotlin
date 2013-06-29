// OPTION: 0
fun foo(n: Int): Int {
    try {
        n / 0
    }
    <caret>catch (e: ArithmeticException) {
        val m = -1
        m
    }
    catch (e: Exception) {
        -2
    }
    finally {

    }

    return 0
}
