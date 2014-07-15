
fun foo() : Long {
    val x = LongArray(5)

    for (i in 0..4) {
        x[i] = (i + 1).toLong()
    }

    return x.fold(0L) { x, y -> x + y }
}

// 0 valueOf
// 0 Value\s\(\)
