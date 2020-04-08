interface B {
    operator fun invoke(x: Int): String
}
class A {
    fun foo(x: Int) {}

    val bar: B = TODO()
}

fun A.foo(x: String) {}

fun main() {
    val a = A()
    a.<!DEBUG_INFO_CALL("fqName: A.foo; typeCall: function")!>foo(1)<!>
    a.<!DEBUG_INFO_CALL("fqName: foo; typeCall: extension function")!>foo("")<!>
    a.<!DEBUG_INFO_CALL("fqName: B.invoke; typeCall: variable&invoke")!>bar(1)<!>
}
