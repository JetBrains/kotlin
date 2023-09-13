// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// FULL_JDK

// MODULE: common
// FILE: common.kt
/*
 * During JVM compilation compiler creates mapped function for java.util.HashMap.contains twice (because they are session-dependant)
 * The same happens with renamed synthetic properties based on real kotlin properties (LinkedHashMap.keySet() -> LinkedHashMap.keys)
 */
fun boxCommon(x: LinkedHashMap<Int, Int>?): String {
    val res = hashSetOf<Int>().contains(12)
    x?.keys
    return if (res) {
        "Error"
    } else {
        "O"
    }
}

// MODULE: platform()()(common)
// FILE: platform.kt
fun boxPlatform(x: LinkedHashMap<Int, Int>?): String {
    val res = hashSetOf<Int>().contains(12)
    x?.keys
    return if (res) {
        "Error"
    } else {
        "K"
    }
}

fun box(): String {
    return boxCommon(null) + boxPlatform(null)
}
