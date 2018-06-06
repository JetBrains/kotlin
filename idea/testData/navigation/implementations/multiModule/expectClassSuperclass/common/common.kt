package test

open class <caret>SimpleParent

expect open class ExpectedChild : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()

// REF: [testModule_Common] (test).ExpectedChild
// REF: [testModule_Common] (test).ExpectedChildChild
// REF: [testModule_Common] (test).SimpleChild
// REF: [testModule_JVM] (test).ExpectedChild
// REF: [testModule_JVM] (test).ExpectedChildChildJvm