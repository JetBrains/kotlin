// RUN_PIPELINE_TILL: FRONTEND

class Foo<T : Any> {
    fun doSmthng(arg: T) {}
    var a: T? = null
}

fun <T : Any> myBuilder(block: Foo<T>.() -> Unit) : Foo<T> = Foo<T>().apply(block)

fun main(arg: Any) {
    val x = 57
    val value = myBuilder {
        doSmthng("one ")
        run { a; this }.a = 10
        a <!NONE_APPLICABLE!>+=<!> 1
        this.a = 57
        this.<!ILLEGAL_SELECTOR, VARIABLE_EXPECTED!>(a)<!> = 57
        a = x
        <!WRAPPED_LHS_IN_ASSIGNMENT_ERROR!>(a)<!> = x
        a.<!FUNCTION_CALL_EXPECTED, VARIABLE_EXPECTED!>hashCode<!> = 99
        if (arg is String) {
            a = arg
        }
    }
    println(value.a?.<!NONE_APPLICABLE!>count<!> { <!UNRESOLVED_REFERENCE!>it<!> in 'l' .. 'q' })
}
