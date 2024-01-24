// TARGET_BACKEND: JVM

// FILE: removeOverriddenInJava_Map.kt

open class MapA : Map<String, String> {
    override val entries: Set<Map.Entry<String, String>> get() = null!!
    override val keys: Set<String> get() = null!!
    override val size: Int get() = null!!
    override val values: Collection<String> get() = null!!
    override fun containsKey(key: String): Boolean = null!!
    override fun containsValue(value: String): Boolean = null!!
    override fun get(key: String): String? = null!!
    override fun isEmpty(): Boolean = null!!
}

fun box() = MapB().remove("OK")

// FILE: MapB.java
public class MapB extends MapA {
    @Override
    public String remove(Object key) {
        return (String) key;
    }
}
