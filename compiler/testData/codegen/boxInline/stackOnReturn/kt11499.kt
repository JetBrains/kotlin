// FILE: 1.kt
object CrashMe {
    fun <T> crash(value: T): T? = null
}

internal inline fun <reified T> crashMe(value: T?): T? {
    return CrashMe.crash(value ?: return null)
}

// FILE: 2.kt
fun box(): String =
    crashMe<String>(null) ?: "OK"
