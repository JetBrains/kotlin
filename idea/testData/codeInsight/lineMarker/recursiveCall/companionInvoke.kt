class Example(val dummy: Any?) {
    companion object {
        operator fun invoke() = <lineMarker>Example</lineMarker>()
    }
}