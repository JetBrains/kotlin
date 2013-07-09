// OPTION: 0
fun foo(n: Int): Int {
    try {
        n / 0
    }
    catch (e: ArithmeticException) {
        -1
    }
    catch (e: Exception) {
        -2
    }
    <caret>finally {
        val s = "finally"
        println(s)
    }

    return 0
}
