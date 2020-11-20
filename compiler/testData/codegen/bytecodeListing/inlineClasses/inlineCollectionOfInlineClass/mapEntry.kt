// IGNORE_ANNOTATIONS

inline class IK(val x: Int)
inline class IV(val x: Double)

inline class InlineMapEntry(private val e: Map.Entry<IK, IV>) : Map.Entry<IK, IV> {
    override val key: IK get() = e.key
    override val value: IV get() = e.value
}
