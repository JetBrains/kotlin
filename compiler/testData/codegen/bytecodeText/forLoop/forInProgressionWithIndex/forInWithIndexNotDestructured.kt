fun box(): String {
    for (iv in (4..7).withIndex()) {
    }

    return "OK"
}

// We do not optimize `withIndex()` if the loop variable is not destructured

// 1 withIndex
// 1 iterator
// 1 hasNext
// 1 next
// 0 component1
// 0 component2
