enum class Foo {
    X
    Y
}

fun foo(f: Foo, i: Int){}

fun bar(){
    foo(<caret>)
}

// ELEMENT: X
