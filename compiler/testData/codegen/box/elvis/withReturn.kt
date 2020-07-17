fun problematic(s: String): String {
    return s.toNullable()?.id() ?: return "fail"
}

fun String.toNullable(): String? = this

fun String.id() = this

fun box() = problematic("OK")
