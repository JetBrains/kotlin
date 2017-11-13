
inline fun foo() {
    try {
        try {
            throw Exception("XXX")
        } catch (e: Throwable) {
            println("Gotcha1: ${e.message}")
            throw Exception("YYY")
        }
    } catch (e: Throwable) {
        println("Gotcha2: ${e.message}")
    }
}
