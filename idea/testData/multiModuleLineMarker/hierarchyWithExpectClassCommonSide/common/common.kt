package test

open class <lineMarker>SimpleParent</lineMarker>

expect open class <lineMarker><lineMarker>ExpectedChild</lineMarker></lineMarker> : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()