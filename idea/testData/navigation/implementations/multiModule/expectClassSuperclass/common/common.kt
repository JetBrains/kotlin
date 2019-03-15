package test

open class <caret>SimpleParent

expect open class ExpectedChild : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()

// DISTINCT_REF
// REF: [testModule_Common] (test).ExpectedChild
// REF: [testModule_Common] (test).ExpectedChildChild
// REF: [testModule_Common] (test).SimpleChild
// REF: [testModule_JVM] (test).ExpectedChild
// REF: [testModule_JVM] (test).ExpectedChildChildJvm