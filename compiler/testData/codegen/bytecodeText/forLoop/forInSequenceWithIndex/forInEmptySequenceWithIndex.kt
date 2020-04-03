// IGNORE_BACKEND: JVM_IR
// TODO KT-36774 Generate optimized loops over 'xs.withIndex()' in JVM_IR
// TODO: Handle Sequences by extending DefaultIterableHandler.
val xs = listOf<Any>().asSequence()

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in xs.withIndex()) {
        return "Loop over empty list should not be executed"
    }
    return "OK"
}

// 0 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// The 1st ICONST_0 is for initializing the list. 2nd is for initializing the index in the lowered for-loop.
// 2 ICONST_0
