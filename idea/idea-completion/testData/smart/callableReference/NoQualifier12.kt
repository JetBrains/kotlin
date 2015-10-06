class C {
    fun foo(p: C.() -> Any){}
    fun foo(p: () -> Any){}

    fun bar() {
        foo(<caret>)
    }

    fun f1(){}
    fun C.f2(){}

    inner class Inner
    class Nested
}

class Outer

// ABSENT: ::f1
// ABSENT: ::f2
// ABSENT: ::Inner
// EXIST: ::Nested
// EXIST: ::Outer
