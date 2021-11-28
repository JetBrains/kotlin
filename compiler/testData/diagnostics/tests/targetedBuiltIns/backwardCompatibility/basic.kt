// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -PLATFORM_CLASS_MAPPED_TO_KOTLIN

abstract class Test1 : Map<String, String> {
    fun <!VIRTUAL_MEMBER_HIDDEN!>containsKey<!>(key: String): Boolean = TODO()

    fun getOrDefault(key: String, defaultValue: String): String = TODO()
}

abstract class Test2 : MutableMap<String, String> {
    fun replace(key: String, value: String): String? = TODO()
}

abstract class Test3 : java.util.AbstractMap<String, String>() {
    fun <!VIRTUAL_MEMBER_HIDDEN!>containsKey<!>(key: String): Boolean = TODO()

    fun replace(key: String, value: String): String? = TODO()
}

