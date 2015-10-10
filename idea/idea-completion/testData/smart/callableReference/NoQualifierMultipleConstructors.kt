class A {
    fun foo(p: (Int) -> Any){}

    fun bar() {
        foo(<caret>)
    }
}

class B() {
    constructor(p: Int) : this(){}
}

// EXIST: ::B
