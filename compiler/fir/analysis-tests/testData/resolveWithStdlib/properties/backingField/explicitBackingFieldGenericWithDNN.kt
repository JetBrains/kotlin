// RUN_PIPELINE_TILL: FRONTEND

open class A<T>(val value: T&Any) {
    val prop: T?
        field: T&Any = value

    fun <S> foo(other: A<S>): S&Any = other.prop
    fun bar(other: A<T>): T&Any = other.prop
    fun baz(other: A<Int?>): Int = other.prop
}

class Child: A<String?>("OK") {
    fun <S> test1(other: A<S>): S&Any = <!RETURN_TYPE_MISMATCH!>other.prop<!>
    fun test2(other: A<Int?>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>
    fun test3(): String = <!RETURN_TYPE_MISMATCH!>prop<!>
    fun test4(): String = <!RETURN_TYPE_MISMATCH!>super.prop<!>
}

fun <S> foo(other: A<S>): S = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun baz(other: A<Int?>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, explicitBackingField, functionDeclaration, nullableType,
primaryConstructor, propertyDeclaration, smartcast, stringLiteral, typeParameter */
