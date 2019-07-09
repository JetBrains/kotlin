class Example(val dummy: Any?) {
    companion object {
        operator fun invoke(): Example = <lineMarker>Example</lineMarker>()
    }
}