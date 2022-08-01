fun test() {
    val x = try {
        4
    } catch (<expr>e: Exception</expr>) {
        5
    } finally {
        9
    }
}