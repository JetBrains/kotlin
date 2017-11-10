fun bar(x: Array<Int>?, y: Int?) {
    if (y!! in x!!) {
        y.inc()
        x.size
    } else {
        y.inc()
        x.size
    }
    y.inc()
    x.size
}