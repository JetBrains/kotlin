interface B {
    operator fun invoke(x: Int): String
}
class A {
    fun foo(x: Int) {
        fun baz(x: Double) {}
        <!DEBUG_INFO_CALL("fqName: A.foo.baz; typeCall: function")!>baz(1.0)<!>
    }

    val bar: B = TODO()
}

fun A.foo(x: String) {}

fun main() {
    fun A.foo(x: Double) {}
    val a = A()
    a.<!DEBUG_INFO_CALL("fqName: A.foo; typeCall: function")!>foo(1)<!>
    a.<!DEBUG_INFO_CALL("fqName: foo; typeCall: extension function")!>foo("")<!>
    a.<!DEBUG_INFO_CALL("fqName: main.foo; typeCall: extension function")!>foo(1.0)<!>

    a.<!DEBUG_INFO_CALL("fqName: B.invoke; typeCall: variable&invoke")!>bar(1)<!>
}
