fun test(b: Any?): Int {
    <expr>b is Number</expr>
    if (b is String) {
        return 54
    } else if (b is Int) {
        return 87
    } else {
        return 0
    }
}