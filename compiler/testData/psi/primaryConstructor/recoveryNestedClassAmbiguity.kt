class Outer1 {
    class Nested1

    private @Ann ()
}

class Outer2 {
    class Nested2 private @Ann
    fun foo() {}
}

class Outer3 {
    class Nested3 private @Ann {}
    fun foo()
}

class Outer4 {
    class Nested3 private @Ann() {}
    fun foo()
}

class Outer5 {
    class Nested3 private @Ann() : Base()
    fun foo()
}

class Outer6 {
    class Nested1
    private Ann ()
}
