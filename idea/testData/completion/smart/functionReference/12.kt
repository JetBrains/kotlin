class C {
    fun foo(p: C.() -> Unit){}

    fun bar() {
        foo(<caret>)
    }

    fun f1(){}
    fun C.f2(){}
}

// EXIST: ::f1
// ABSENT: ::f2
