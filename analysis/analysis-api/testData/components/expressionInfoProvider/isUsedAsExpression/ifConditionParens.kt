fun test(b: Any?): Int {
    if (<expr>(b is Boolean)</expr>) {
        return 54
    } else if (b is Int) {
        return 87
    } else {
        return 0
    }
}