// IGNORE_BACKEND: JVM_IR
//WITH_RUNTIME
class Test {

    data class Style(
            val color: Int? = null,
            val underlined: Boolean? = null,
            val separator: String = ""
    )

    init {
        var flag: Boolean? = null

        val receiver: String = "123"
        try {
            receiver.let { a2 ->
                flag = false
            }
        } finally {
            receiver.hashCode()
        }
        val style = Style(null, flag, "123")
    }
}


fun box(): String {
    Test()

    return "OK"
}