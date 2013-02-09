fun z(b: Boolean) {}

fun foo(b: Boolean) {
    if (b) {
        z(b)
    } else {
        z(b)
    }
}

// 0 GETSTATIC
