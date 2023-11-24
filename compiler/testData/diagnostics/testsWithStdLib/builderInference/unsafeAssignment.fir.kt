class Foo<T : Any> {
    fun doSmthng(arg: T) {}
    var a: T? = null
}

fun <T : Any> myBuilder(block: Foo<T>.() -> Unit) : Foo<T> = Foo<T>().apply(block)

fun main(arg: Any) {
    val x = 57
    val value = myBuilder {
        doSmthng("one ")
        run { a; this }.<!UNRESOLVED_REFERENCE!>a<!> = 10
        <!BUILDER_INFERENCE_STUB_RECEIVER!>a<!> += 1
        this.a = <!ASSIGNMENT_TYPE_MISMATCH!>57<!>
        this.<!ILLEGAL_SELECTOR, VARIABLE_EXPECTED!>(a)<!> = 57
        a = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
        (a) = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
        a.<!FUNCTION_CALL_EXPECTED, VARIABLE_EXPECTED!>hashCode<!> = 99
        if (arg is String) {
            a = arg
        }
    }
    println(value.a?.count { it in 'l' .. 'q' })
}
