fun test(b: Any?): Int {
    if (b is <expr>Number</expr>) {
        return 54
    } else if (b is Int) {
        return 87
    } else {
        return 0
    }
}