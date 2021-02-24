// IGNORE_ANNOTATIONS

inline class IK(val x: Int)
inline class IV(val x: Double)

inline class InlineMutableMapEntry(private val e: MutableMap.MutableEntry<IK, IV>) : MutableMap.MutableEntry<IK, IV> {
    override val key: IK get() = e.key
    override val value: IV get() = e.value
    override fun setValue(newValue: IV): IV = e.setValue(newValue)
}
