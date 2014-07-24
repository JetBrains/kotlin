public inline fun Int.times2(body : () -> Unit) {
    var count = this;
    while (count > 0) {
        body()
        count--
    }
}