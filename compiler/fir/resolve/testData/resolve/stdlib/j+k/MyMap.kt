// FULL_JDK
// FILE: MyMap.java

public abstract class MyMap implements java.util.Map<String, String> {}

// FILE: test.kt
fun test(map: MyMap) {
    val result = map.getOrPut("key") { "value" } // Cannot be resolved without early J2K mapping
    // In contrast, should be taken from JDK
    val otherResult = map.getOrDefault("key", "value")
    val anotherResult = map.<!UNRESOLVED_REFERENCE!>replace<!>("key", "value")
    // Java forEach
    map.forEach { key, value ->
        println("$key: $value")
    }
    // Kotlin forEach
    map.forEach { (key, value) ->
        println("$key: $value")
    }
}

fun test(map: MutableMap<String, String>) {
    val result = map.getOrPut("key") { "value" } // Cannot be resolved without early J2K mapping
    // In contrast, should be taken from JDK
    val otherResult = map.getOrDefault("key", "value")
    val anotherResult = map.<!UNRESOLVED_REFERENCE!>replace<!>("key", "value")
    // Java forEach
    map.forEach { key, value ->
        println("$key: $value")
    }
    // Kotlin forEach
    map.forEach { (key, value) ->
        println("$key: $value")
    }
}