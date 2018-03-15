// WITH_RUNTIME

fun box(): String {
    lateinit var str: String
    try {
        println(str)
        return "Should throw an exception"
    }
    catch (e: UninitializedPropertyAccessException) {
        return "OK"
    }
    catch (e: Throwable) {
        return "Unexpected exception: ${e::class}"
    }
}