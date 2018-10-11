package test

open class SimpleParent

expect open class <caret>ExpectedChild : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()

// REF: [testModule_Common] (test).ExpectedChildChild
// REF: [testModule_JVM] (test).ExpectedChild
// REF: [testModule_JVM] (test).ExpectedChildChildJvm