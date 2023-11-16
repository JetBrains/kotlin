// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
annotation class Key(val value: String)

object Messanger {
    const val DEFAULT_TEXT = <!EVALUATED("OK")!>"OK"<!>

    fun message(@Key(value = <!EVALUATED("OK")!>DEFAULT_TEXT<!>) text: String = <!EVALUATED("OK")!>DEFAULT_TEXT<!>): String {
        return text
    }
}

fun box(): String {
    return Messanger.message()
}
