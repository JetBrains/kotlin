// http://youtrack.jetbrains.net/issue/KT-526
// KT-526 Unresolved reference for inner static class

package demo

class Foo {
    companion object {
        class Bar() { }
    }
}
class User {
    fun main() : Unit {
        var <!UNUSED_VARIABLE!>boo<!> : Foo.Companion.Bar? /* <-- this reference is red */ = Foo.Companion.Bar()
    }
}
