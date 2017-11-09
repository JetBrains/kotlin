open class <lineMarker>Super</lineMarker> {
    open fun <lineMarker>f</lineMarker>(a: Int) {
    }
}

class F: Super() {
    override fun <lineMarker>f</lineMarker>(a: Int) {
        if (a > 0) {
            super.f(a - 1)
        }
    }
}
