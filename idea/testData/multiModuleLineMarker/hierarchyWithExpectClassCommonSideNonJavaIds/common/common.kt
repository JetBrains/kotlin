package test

open class <lineMarker>SimpleParent</lineMarker> {
    open fun <lineMarker>`foo fun`</lineMarker>(n: Int) {}
    open val <lineMarker>`bar fun`</lineMarker>: Int get() = 1
}

expect open class <lineMarker><lineMarker>ExpectedChild</lineMarker></lineMarker> : SimpleParent {
    override fun <lineMarker><lineMarker>`foo fun`</lineMarker></lineMarker>(n: Int)
    override val <lineMarker><lineMarker>`bar fun`</lineMarker></lineMarker>: Int
}

class ExpectedChildChild : ExpectedChild() {
    override fun <lineMarker>`foo fun`</lineMarker>(n: Int) {}
    override val <lineMarker>`bar fun`</lineMarker>: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override fun <lineMarker>`foo fun`</lineMarker>(n: Int) {}
    override val <lineMarker>`bar fun`</lineMarker>: Int get() = 1
}