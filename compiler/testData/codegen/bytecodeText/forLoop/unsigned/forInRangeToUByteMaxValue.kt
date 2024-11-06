fun f(a: UByte): Int {
    var n = 0
    for (b in a..UByte.MAX_VALUE) {
        n++
    }
    return n
}

// 0 iterator
// 0 hasNext
// 0 next
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// 3 SIPUSH 255
// 5 ILOAD
// 3 ISTORE
// 0 IADD
// 0 ISUB
// 2 IINC