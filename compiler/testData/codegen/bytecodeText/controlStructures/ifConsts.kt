fun foo(b: Boolean): Int {
    return if (b) {
        100
    } else if (false) {
        101
    } else if (true) {
        102
    } else if (true) {
        103
    } else if (b) {
        104
    } else {
        105
    }
}

// 2 BIPUSH
// 1 BIPUSH 100
// 1 BIPUSH 102
