fun foo() {
    val klass = MyClass()
    klass.<caret>bar()
}

class MyClass {
    fun bar() = 1
}

// EXPECTED: null