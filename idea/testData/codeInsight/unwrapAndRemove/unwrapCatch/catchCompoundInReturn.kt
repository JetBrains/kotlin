// IS_APPLICABLE: false
fun foo(n: Int): Int {
    return try {
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
}
