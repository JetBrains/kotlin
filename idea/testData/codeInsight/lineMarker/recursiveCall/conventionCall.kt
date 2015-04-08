fun Any.get(a: Int) {
    if (a > 0) {
        <lineMarker>this[a - 1]</lineMarker>
    }
}