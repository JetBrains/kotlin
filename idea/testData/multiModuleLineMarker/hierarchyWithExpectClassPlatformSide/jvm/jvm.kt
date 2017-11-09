package test

actual open class <lineMarker><lineMarker>ExpectedChild</lineMarker></lineMarker> : SimpleParent() {
    actual override fun <lineMarker><lineMarker><lineMarker>foo</lineMarker></lineMarker></lineMarker>(n: Int) {}
    actual override val <lineMarker><lineMarker><lineMarker>bar</lineMarker></lineMarker></lineMarker>: Int get() = 1
}

class ExpectedChildChildJvm : ExpectedChild() {
    override fun <lineMarker>foo</lineMarker>(n: Int) {}
    override val <lineMarker>bar</lineMarker>: Int get() = 1
}