package test

open class SimpleParent

expect open class <caret>ExpectedChild : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()

// REF: [common] (test).ExpectedChildChild
// REF: [jvm] (test).ExpectedChildChildJvm