open class A<out T> {
    private open fun foo(t: T) {}
}

class B : A<String>() {
    <caret>
}