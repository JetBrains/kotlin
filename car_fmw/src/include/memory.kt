
val MEMORY_STATIC_HEAP = 0
val MEMORY_DYNAMIC_HEAP = 1

external fun set_active_heap(heap: Int)
external fun clean_dynamic_heap()