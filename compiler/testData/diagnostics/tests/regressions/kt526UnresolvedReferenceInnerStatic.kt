// http://youtrack.jetbrains.net/issue/KT-526
// KT-526 Unresolved reference for inner static class

package demo

class Foo {
    default object {
        class Bar() { }
    }
}
class User {
    fun main() : Unit {
        var <!UNUSED_VARIABLE!>boo<!> : Foo.Default.Bar? /* <-- this reference is red */ = Foo.Default.Bar()
    }
}
