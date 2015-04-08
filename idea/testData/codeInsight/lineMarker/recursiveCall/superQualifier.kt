open class <lineMarker></lineMarker>Super {
    open fun <lineMarker></lineMarker>f(a: Int) {
    }
}

class F: Super() {
    override fun <lineMarker></lineMarker>f(a: Int) {
        if (a > 0) {
            super.f(a - 1)
        }
    }
}
