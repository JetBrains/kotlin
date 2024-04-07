abstract class A : List<Any> {
    override fun <!OVERRIDE_DEPRECATION!>getFirst<!>(): Any {
        return super.<!DEPRECATION!>getFirst<!>()
    }
}

fun <T> List<T>.getFirst(): Int = 1

fun test(l: A){
    consumeInt(<!ARGUMENT_TYPE_MISMATCH!>l.<!DEPRECATION!>getFirst<!>()<!>)

}
fun consumeInt(i: Int) {}
