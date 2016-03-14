fun foo(arg: Any?): Int {
    // 1
    <caret>if (arg is Int) {
        return arg // 2
    }
    // 3
    if (arg is String) {
        return 42 // 4
    }
    // 5
    if (arg == null) {
        // 6
        return 0
    }
    // 7
    return -1 // 8
}