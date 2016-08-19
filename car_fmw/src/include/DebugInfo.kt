
external fun dynamic_heap_tail(): Int
external fun static_heap_tail(): Int
external fun dynamic_heap_max_bytes(): Int
external fun dynamic_heap_total(): Int

object DebugInfo {
    fun getDynamicHeapTail(): Int = dynamic_heap_tail()
    fun getStaticHeapTail(): Int = static_heap_tail()
    fun getDynamicHeapMaxSize(): Int = dynamic_heap_max_bytes()
    fun getDynamicHeapTotalBytes(): Int = dynamic_heap_total()
}