fun foo() {
    val (x, private data @ann [ann] y) = pair
    val ([ann], x) = pair

    @volatile val (@ann x, y) = 1
    @volatile val (@ann x, y = 1
}
