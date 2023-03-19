// IGNORE_ANNOTATIONS

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57268 K2: extra methods `remove` and/or `getOrDefault` are generated for Map subclasses with JDK 1.6 in dependencies
// (in this case, it's `getOrDefault-h8vw2VU` because of inline class mangling, and then `remove` is unmangled for some reason)

inline class IK(val x: Int)
inline class IV(val x: Double)

inline class InlineMap(private val map: Map<IK, IV>) : Map<IK, IV> {
    override val entries: Set<Map.Entry<IK, IV>> get() = map.entries
    override val keys: Set<IK> get() = map.keys
    override val size: Int get() = map.size
    override val values: Collection<IV> get() = map.values
    override fun containsKey(key: IK): Boolean = map.containsKey(key)
    override fun containsValue(value: IV): Boolean = map.containsValue(value)
    override fun get(key: IK): IV? = map[key]
    override fun isEmpty(): Boolean = map.isEmpty()
}
