// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

open class Base

class TestImplicitPrimaryConstructor : Base()

class TestExplicitPrimaryConstructor() : Base()

class TestWithDelegatingConstructor(val x: Int, val y: Int) : Base() {
    constructor(x: Int) : this(x, 0)
}

