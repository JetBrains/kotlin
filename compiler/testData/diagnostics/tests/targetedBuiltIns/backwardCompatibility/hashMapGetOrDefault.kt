// !DIAGNOSTICS: -UNUSED_PARAMETER -PLATFORM_CLASS_MAPPED_TO_KOTLIN
// JAVAC_EXPECTED_FILE

class MyHashMap : java.util.HashMap<String, String>() {
    fun getOrDefault(key: String, defaultValue: String): String = TODO()
}