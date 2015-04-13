class X{
    fun foo(p: () -> Unit){}

    fun bar() {
        foo(this.<caret>)
    }

    fun f(){}
}

// NUMBER: 0