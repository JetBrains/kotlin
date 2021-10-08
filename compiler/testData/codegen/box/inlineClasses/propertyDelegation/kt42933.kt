// WITH_RUNTIME

class Delegate {
    operator fun getValue(t: Any?, p: Any): String = "OK"
}

@JvmInline
value class Kla1(val default: Int) {
    fun getValue(): String {
        val prop by Delegate()
        return prop
    }
}

fun box() = Kla1(1).getValue()