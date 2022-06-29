// TARGET_BACKEND: JVM
// WITH_STDLIB
// IGNORE_BACKEND: JVM
// FILE: lib.kt

abstract class AbstractStringMap<K> : Map<K, String>
class KotlinMap<K> : AbstractStringMap<K>(), MutableMap<K, String> by mutableMapOf()

// FILE: UseMap.java

public class UseMap {
    public static <K> String first(AbstractStringMap<K> map) {
        return map.getEntries().iterator().next().getValue();
    }
}

// FILE: main.kt

fun box(): String {
    val map = KotlinMap<Int>()
    map[0] = "OK"
    return UseMap.first(map)
}
