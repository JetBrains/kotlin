// "Change 'bar' function return type to 'A'" "true"
fun foo() {
    open class A

    fun bar(): A {
        return object: A() {}
    }
}