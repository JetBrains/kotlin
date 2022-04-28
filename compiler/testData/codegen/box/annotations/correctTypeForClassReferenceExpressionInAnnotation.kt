// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-52190

fun box(): String {
    buildMap {
        val replaced = put("key", "value")
        if (replaced != null) {
            return "Error: $replaced"
        }
    }
    return "OK"
}