fun box(): String {
    for ((outer, iv) in (4..7).withIndex().withIndex()) {
    }

    return "OK"
}

// We optimize the outer `withIndex()` and treat the inner one as an Iterable

// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2

// The ICONST_0 is for initializing the index in the lowered for-loop.
// 1 ICONST_0
