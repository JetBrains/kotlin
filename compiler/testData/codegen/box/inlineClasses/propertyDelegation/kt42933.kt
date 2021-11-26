// WITH_STDLIB

class Delegate {
    operator fun getValue(t: Any?, p: Any): String = "OK"
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Kla1(val default: Int) {
    fun getValue(): String {
        val prop by Delegate()
        return prop
    }
}

fun box() = Kla1(1).getValue()