
abstract class A : List<Any> {
    override fun <!OVERRIDE_DEPRECATION!>getFirst<!>(): Any {
        return super.<!DEPRECATION, JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>getFirst<!>()
    }
}

abstract class B : List<Any>

class Test {
    fun <T> List<T>.<!EXTENSION_SHADOWED_BY_MEMBER!>getFirst<!>() = 1

    fun test(a: A, b: B){
        consumeInt(<!TYPE_MISMATCH!>a.getFirst()<!>)
        consumeInt(b.getFirst())
    }
}
fun consumeInt(i: Int) {}