fun box(): String {
    for ((index, x) in "".withIndex()) {
        return "Loop over empty String should not be executed"
    }
    return "OK"
}

// 0 withIndex
// 0 iterator
// 0 hasNext
// 0 next
// 0 component1
// 0 component2
// 1 length
// 1 charAt

// The ICONST_0 is for initializing the index in the lowered for-loop.
// 1 ICONST_0
