// TARGET_BACKEND: JVM_IR
annotation class Key(val value: String)

object Messanger {
    const val DEFAULT_TEXT = "OK"

    fun message(@Key(value = DEFAULT_TEXT) text: String = DEFAULT_TEXT): String {
        return text
    }
}

fun box(): String {
    return Messanger.message()
}
