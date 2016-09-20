// "Change return type of current function 'bar' to 'A'" "true"
fun foo() {
    open class A

    fun bar(): Int {
        return <caret>object: A() {}
    }
}