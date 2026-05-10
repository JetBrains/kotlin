// RUN_PIPELINE_TILL: FRONTEND

class A<T: Number>(val value: T) {
    val prop: Any?
        field: T = value

    fun <S: Number> foo(other: A<S>): Number = other.prop
    fun bar(other: A<T>): Number = other.prop
    fun baz(other: A<Int>): Number = other.prop
    fun <S: Number> A<S>.qux(): Number = this.prop
    fun A<T>.quux(): Number = this@quux.prop
}

class B<T>(val value: T) where T: Number, T: Comparable<T> {
    val prop: Any?
        field: T = value

    fun <S> foo(other: B<S>): Number where S : Number, S : Comparable<S> = other.prop
    fun bar(other: B<T>): Number = other.prop
    fun baz(other: B<Int>): Number = other.prop
}

fun <S: Number> test1(other: A<S>): Number = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun test2(other: A<Int>): Number = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun <S> test3(other: B<S>): Number where S : Number, S : Comparable<S> = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun test4(other: B<Int>): Number = <!RETURN_TYPE_MISMATCH!>other.prop<!>

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, smartcast, typeConstraint, typeParameter */
