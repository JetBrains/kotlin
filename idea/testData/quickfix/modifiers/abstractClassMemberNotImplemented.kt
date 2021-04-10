// "Make 'Foo' 'abstract'" "true"

abstract class Bar {
    abstract val i: Int
}

class <caret>Foo : Bar() {}