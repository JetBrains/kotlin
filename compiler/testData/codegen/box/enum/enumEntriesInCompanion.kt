// LANGUAGE: +EnumEntries
// IGNORE_BACKEND: JVM
// WITH_STDLIB

enum class MyEnum {
    OK, NOPE;

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        val ok = entries[0]
    }
}

fun box(): String {
    return MyEnum.ok.toString()
}