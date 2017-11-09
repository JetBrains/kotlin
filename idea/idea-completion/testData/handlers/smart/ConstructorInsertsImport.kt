class X<T> {
    fun foo(p: java.util.TreeMap<java.io.File, T>){}

    fun f(){
        foo(<caret>)
    }
}

// ELEMENT: TreeMap
