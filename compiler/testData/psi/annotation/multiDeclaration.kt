fun foo() {
    val (x, private data @ann @[ann] y) = pair
    val (@[ann], x) = pair

    @Volatile val (@ann x, y) = 1
    @Volatile val (@ann x, y = 1
}
