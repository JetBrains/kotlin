fun box(): String {
    for ((i, v) in listOf(4, 5, 6, 7).indices.withIndex()) {
    }

    return "OK"
}

// 0 withIndex
// 0 component1
// 0 component2

// The 1st ICONST_0 is for initializing the list. 2nd is for initializing the index in the lowered for-loop.
// 2 ICONST_0

// JVM_TEMPLATES
// 1 iterator
// 1 hasNext
// 1 next

// JVM_IR_TEMPLATES
// 0 iterator
// 0 hasNext
// 0 next
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 getIndices
