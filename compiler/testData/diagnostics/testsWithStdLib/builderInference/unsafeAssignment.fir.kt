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
        a += 1
        this.a = 57
        this.<!ILLEGAL_SELECTOR, VARIABLE_EXPECTED!>(a)<!> = 57
        a = x
        (a) = x
        a.<!FUNCTION_CALL_EXPECTED, VARIABLE_EXPECTED!>hashCode<!> = 99
        if (arg is String) {
            a = arg
        }
    }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(value.a?.<!UNRESOLVED_REFERENCE!>count<!> { <!UNRESOLVED_REFERENCE!>it<!> in 'l' .. 'q' })
}
