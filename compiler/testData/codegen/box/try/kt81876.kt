// TARGET_BACKEND: JVM

fun box():String{
    try {
        run {
             return "OK"
        }
    } finally {
        try {
            throw RuntimeException()
        } catch (e: Exception) {
        }
    }
}
