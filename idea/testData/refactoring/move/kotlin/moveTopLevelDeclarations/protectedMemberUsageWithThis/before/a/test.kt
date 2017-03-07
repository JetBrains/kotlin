package a

open class ProtectedParent {
    protected open fun inherit() {}
}

class <caret>ProtectedChild : ProtectedParent() {
    fun foo() {
        this.inherit()
        inherit()
    }
}