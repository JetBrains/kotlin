// IS_APPLICABLE: false
fun bar() {
    var x: <caret>String
    fun foo() {
        x = "456"
        x.hashCode()
    }
    foo()
    x = "123"
    x.hashCode()
}
