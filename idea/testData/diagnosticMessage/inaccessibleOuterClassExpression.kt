class A {
    class B {
        val r = object {
            fun bar() = this@A
        }
    }
}
