class X<T> {
    fun foo(p: java.util.HashMap<T, java.util.AbstractMap<T, java.io.File>>){}

    fun f(){
        foo(<caret>)
    }
}

// ELEMENT: HashMap
