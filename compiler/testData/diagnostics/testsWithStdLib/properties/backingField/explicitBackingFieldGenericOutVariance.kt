// RUN_PIPELINE_TILL: FRONTEND

open class C<out T>(val value: T) {
    val prop: Any?
        field: T = value

    fun <S> foo(other: C<S>): S = other.prop
    fun bar(other: C<@UnsafeVariance T>): T = other.prop
    fun baz(other: C<Int>): Int = other.prop
    fun baaz(): T = this.prop
    fun <S> C<S>.qux(): S = this.prop
    fun C<@UnsafeVariance T>.quux(): T = this@quux.prop
}

class D: C<String>("OK") {
    fun <S> test1(other: C<S>): S = <!RETURN_TYPE_MISMATCH!>other.prop<!>
    fun test2(other: C<Int>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>
    fun test3(): String = <!RETURN_TYPE_MISMATCH!>prop<!>
    fun test4(): String = <!RETURN_TYPE_MISMATCH!>super.prop<!>
}

fun <S> foo(other: C<S>): S = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun bar(other: C<Int>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, nullableType, out,
primaryConstructor, propertyDeclaration, smartcast, stringLiteral, superExpression, thisExpression, typeParameter */
