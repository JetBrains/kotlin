class Foo<T : Any> {
    fun doSmthng(arg: T) {}
    var a: T? = null
}

fun <T : Any> myBuilder(block: Foo<T>.() -> Unit) : Foo<T> = Foo<T>().apply(block)

fun main(arg: Any) {
    val x = 57
    val value = myBuilder {
        doSmthng("one ")
        run { a; this }.a = <!TYPE_MISMATCH, TYPE_MISMATCH!>10<!>
        <!BUILDER_INFERENCE_STUB_RECEIVER, STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY, TYPE_MISMATCH, TYPE_MISMATCH!>a<!> <!OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>+=<!> 1
        this.a = <!TYPE_MISMATCH!>57<!>
        this.<!ILLEGAL_SELECTOR, VARIABLE_EXPECTED!>(a)<!> = <!TYPE_MISMATCH!>57<!>
        a = <!TYPE_MISMATCH!>x<!>
        (a) = <!TYPE_MISMATCH!>x<!>
        <!TYPE_MISMATCH!>a<!>.<!FUNCTION_CALL_EXPECTED, VARIABLE_EXPECTED!>hashCode<!> = <!TYPE_MISMATCH, TYPE_MISMATCH!>99<!>
        if (arg is String) {
            a = arg
        }
    }
    println(value.a?.count { it in 'l' .. 'q' })
}
