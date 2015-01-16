import kotlin.platform.platformName

class C(i: Int){
    class Nested
    inner class Inner

    fun bar() {
        foo(<caret>)
    }
}

platformName("foo1")
fun foo(p: () -> C.Nested){}

platformName("foo2")
fun foo(p: () -> C.Inner){}

// EXIST: ::Nested
// ABSENT: ::Inner
