class F {
    fun f(a: Int) {
        if (a > 0) {
            this.<lineMarker>f</lineMarker>(a - 1)
        }
    }
}
