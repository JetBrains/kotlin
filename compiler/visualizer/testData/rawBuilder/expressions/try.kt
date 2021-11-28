// WITH_STDLIB
fun some() {
    try {
//            constructor KotlinNullPointerException()
//            │
        throw KotlinNullPointerException()
//              java/lang/RuntimeException
//              │
    } catch (e: RuntimeException) {
//      fun io/println(Any?): Unit
//      │
        println("Runtime exception")
//              java/lang/Exception
//              │
    } catch (e: Exception) {
//      fun io/println(Any?): Unit
//      │
        println("Some exception")
    } finally {
//      fun io/println(Any?): Unit
//      │
        println("finally")
    }
}
