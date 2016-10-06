class X<T> {
    fun foo(p: java.util.TreeMap<T, java.util.AbstractMap<T, java.io.File>>){}

    fun f(){
        foo(<caret>)
    }
}

// ELEMENT: TreeMap
