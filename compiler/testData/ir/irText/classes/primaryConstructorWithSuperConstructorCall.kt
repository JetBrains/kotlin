open class Base

class TestImplicitPrimaryConstructor : Base()

class TestExplicitPrimaryConstructor() : Base()

class TestWithDelegatingConstructor(val x: Int, val y: Int) : Base() {
    constructor(x: Int) : this(x, 0)
}

