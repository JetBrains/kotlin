fun main(f: (Int) -> Int) {
    try {
        val x = 4; f(x)
    } catch (e: jet.Throwable) {
        val y = 4
    }
}