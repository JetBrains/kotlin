// FIR_IDENTICAL
// WITH_RUNTIME

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

