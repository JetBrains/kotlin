// !CHECK_HIGHLIGHTING

package test

open class <caret>SimpleParent

expect open class ExpectedChild : SimpleParent

class ExpectedChildChild : ExpectedChild()

class SimpleChild : SimpleParent()