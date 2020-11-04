class A {
    fun test() {
        val a: A
        synchronized(this) {
            if (bar()) throw RuntimeException()
            a = A()
        }
        <!UNINITIALIZED_VARIABLE!>a<!>.bar()
    }

    fun bar() = false
}
