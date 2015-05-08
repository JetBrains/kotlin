// "Change 'bar' function return type to 'A'" "true"
fun foo() {
    open class A

    fun bar(): Int {
        return <caret>object: A() {}
    }
}