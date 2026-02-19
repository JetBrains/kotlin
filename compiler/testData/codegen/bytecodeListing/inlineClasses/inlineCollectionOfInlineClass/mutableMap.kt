// IGNORE_ANNOTATIONS

inline class IK(val x: Int)
inline class IV(val x: Double)

inline class InlineMutableMap(private val mmap: MutableMap<IK, IV>) : MutableMap<IK, IV> {
    override val size: Int get() = mmap.size
    override fun containsKey(key: IK): Boolean = mmap.containsKey(key)
    override fun containsValue(value: IV): Boolean = mmap.containsValue(value)
    override fun get(key: IK): IV? = mmap[key]
    override fun isEmpty(): Boolean = mmap.isEmpty()
    override val entries: MutableSet<MutableMap.MutableEntry<IK, IV>> get() = mmap.entries
    override val keys: MutableSet<IK> get() = mmap.keys
    override val values: MutableCollection<IV> get() = mmap.values
    override fun clear() { mmap.clear() }
    override fun put(key: IK, value: IV): IV? = mmap.put(key, value)
    override fun putAll(from: Map<out IK, IV>) { mmap.putAll(from) }
    override fun remove(key: IK): IV? = mmap.remove(key)
}
