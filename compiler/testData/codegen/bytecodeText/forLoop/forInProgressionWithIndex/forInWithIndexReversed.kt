fun box(): String {
    for ((i, v) in (4..7).withIndex().reversed()) {
    }

    return "OK"
}

// We do not optimize `withIndex().reversed()`

// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 1 component1
// 1 component2
// 1 reversed
