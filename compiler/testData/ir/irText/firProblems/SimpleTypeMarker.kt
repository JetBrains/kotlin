// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

interface SimpleTypeMarker

class SimpleType : SimpleTypeMarker {
    fun foo() = "OK"
}

interface User {
    fun SimpleTypeMarker.bar(): String {
        require(this is SimpleType)
        return this.foo()
    }
}

class UserImpl {
    fun SimpleTypeMarker.bar(): String {
        require(this is SimpleType)
        return this.foo()
    }
}

