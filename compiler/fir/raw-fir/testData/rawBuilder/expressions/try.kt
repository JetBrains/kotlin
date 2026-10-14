// WITH_STDLIB
fun some() {
    try {
        throw KotlinNullPointerException()
    } catch (e: RuntimeException) {
        println("Runtime exception")
    } catch (e: Exception) {
        println("Some exception")
    } finally {
        println("finally")
    }
}