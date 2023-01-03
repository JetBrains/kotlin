package foo

interface I {
    fun foo() = 23
}

class Sub : I {
    var foo = 42
}
