fun foo() {
    val klass = MyClass()
    <caret>klass.bar()
}

class MyClass {
    fun bar() = 1
}

// EXPECTED: klass