fun box(): String {
    var i = 0
    {
        if (1 == 1) {
            i++
        } else {
        }
    }.let { it() }
    return "OK"
}
