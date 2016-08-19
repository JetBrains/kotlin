
external fun set_active_heap(heap: Int)
external fun clean_dynamic_heap()

object Memory {
    val STATIC_HEAP = 0
    val DYNAMIC_HEAP = 1

    fun setHeap(heap: Int) {
        set_active_heap(heap)
    }

    fun cleanDynamicHeap() {
        clean_dynamic_heap()
    }
}
