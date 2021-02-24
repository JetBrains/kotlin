// TARGET_BACKEND: JVM

// java.lang.NoSuchMethodError: java.util.TreeMap.remove
// IGNORE_BACKEND: ANDROID

// FULL_JDK
// JVM_TARGET: 1.8

import java.util.*

private class IntervalTreeMap : TreeMap<String, String>()

fun box(): String {
    val intervalTreeMap = IntervalTreeMap()
    intervalTreeMap.put("123", "356")

    if (!intervalTreeMap.remove("123", "356")) return "fail 1"
    return intervalTreeMap.getOrDefault("123", "OK")
}
