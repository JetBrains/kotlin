fun box(): String {
    for ((i, v) in (4 until 8).withIndex()) {
    }

    return "OK"
}

// 0 withIndex
// 0 component1
// 0 component2

// The ICONST_0 is for initializing the index in the lowered for-loop.
// 1 ICONST_0

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
