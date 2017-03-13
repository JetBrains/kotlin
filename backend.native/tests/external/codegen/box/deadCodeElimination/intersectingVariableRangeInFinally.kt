fun box(): String {
    try {
        return "OK"
    } finally {
        if (1 == 1) {
            val z = 2
        }
        if (3 == 3) {
            val z = 4
        }
    }
}
