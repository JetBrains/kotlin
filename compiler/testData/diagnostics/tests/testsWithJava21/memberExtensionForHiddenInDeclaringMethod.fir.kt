abstract class A : List<Any> {
    override fun <!OVERRIDE_DEPRECATION!>getFirst<!>(): Any {
        return super.<!DEPRECATION!>getFirst<!>()
    }
}

abstract class B : List<Any>

class Test {
    fun <T> List<T>.<!EXTENSION_SHADOWED_BY_MEMBER!>getFirst<!>() = 1

    fun test(a: A, b: B){
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>a.<!DEPRECATION!>getFirst<!>()<!>)
        consumeInt(b.getFirst())
    }
}
fun consumeInt(i: Int) {}
