class A {
    companion {
        fun foo() = 42
        var a = "OK"
    }
}

companion fun A.bar() = 42
companion var A.b = "OK"
