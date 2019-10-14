// FULL_JDK
// FILE: MyMap.java

public abstract class MyMap implements java.util.Map<String, String> {}

// FILE: test.kt
fun test(map: MyMap) {
    val result = map.getOrPut("key") { "value" } // Cannot be resolved without early J2K mapping
}