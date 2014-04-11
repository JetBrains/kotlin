fun foo() {
    val klass = MyClass()
    klass.<caret>bar
}

class MyClass {
    val bar = 1
}

// EXPECTED: klass.bar