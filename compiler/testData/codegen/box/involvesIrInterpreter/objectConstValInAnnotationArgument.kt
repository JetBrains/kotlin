// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

annotation class Key(val value: String)

object Messanger {
    const val DEFAULT_TEXT = <!EVALUATED("OK")!>"OK"<!>

    fun message(@Key(value = <!EVALUATED("OK")!>DEFAULT_TEXT<!>) text: String = <!EVALUATED{IR}("OK")!>DEFAULT_TEXT<!>): String {
        return text
    }
}

fun box(): String {
    return Messanger.message()
}
