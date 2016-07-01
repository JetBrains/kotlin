// VAL
class A(
        val<!SYNTAX!><!>
        val x: Int,
        val
        private<!SYNTAX!><!> val z: Int,
        val<!SYNTAX!><!>
)

val<!SYNTAX!><!>
fun foo() {}

class B {
    val<!SYNTAX!><!>
    fun foo() {}

    fun bar() {
        val<!SYNTAX!><!>
        fun foo() {}
    }
}

// VAR
class C(
        var<!SYNTAX!><!>
        val x: Int,
        var
        private<!SYNTAX!><!> val z: Int,
        var<!SYNTAX!><!>
)

var<!SYNTAX!><!>
fun baz() {}

class D {
    var<!SYNTAX!><!>
    fun foo() {}

    fun bar() {
        var<!SYNTAX!><!>
        fun foo() {}
    }
}
