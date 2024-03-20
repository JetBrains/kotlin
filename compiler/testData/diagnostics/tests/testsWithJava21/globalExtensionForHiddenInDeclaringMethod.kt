abstract class A : List<Any> {
    override fun <!OVERRIDE_DEPRECATION!>getFirst<!>(): Any {
        return <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.getFirst()
    }
}

fun <T> List<T>.<!EXTENSION_SHADOWED_BY_MEMBER!>getFirst<!>(): Int = 1

fun test(l: A){
    consumeInt(<!TYPE_MISMATCH!>l.getFirst()<!>)

}
fun consumeInt(i: Int) {}
