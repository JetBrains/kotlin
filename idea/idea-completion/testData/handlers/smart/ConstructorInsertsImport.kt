class X<T> {
    fun foo(p: java.util.HashMap<java.io.File, T>){}

    fun f(){
        foo(<caret>)
    }
}

// ELEMENT: HashMap
