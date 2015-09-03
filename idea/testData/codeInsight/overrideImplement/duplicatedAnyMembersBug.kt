open class A<T> {
}

interface I

class B : A<String>(), I {
    <caret>
}
