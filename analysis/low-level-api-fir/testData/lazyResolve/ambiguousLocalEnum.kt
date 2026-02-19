// ISSUE: KT-62681

fun someFun(): String {
    enum class EnumClass {
        EnumClass
    }

    return Enum<caret>Class.EnumClass.toString()
}
