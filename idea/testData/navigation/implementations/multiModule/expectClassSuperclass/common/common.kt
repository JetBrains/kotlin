package test

open class <caret>SimpleParent

expect open class ExpectedChild : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()

// REF: [common] (test).ExpectedChild
// REF: [common] (test).ExpectedChild
// REF: [common] (test).ExpectedChildChild
// REF: [common] (test).SimpleChild
// REF: [jvm] (test).ExpectedChild
// REF: [jvm] (test).ExpectedChildChildJvm