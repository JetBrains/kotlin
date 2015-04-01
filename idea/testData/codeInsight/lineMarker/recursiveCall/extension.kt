fun Any.f(a: Int) {
    "".f(1)
    this.<lineMarker>f</lineMarker>(2)
    <lineMarker>f</lineMarker>(3)
}