// RUN_PIPELINE_TILL: FRONTEND

class D<T>(val v: T) {
    val prop: Any?
        field: T = v

    fun <S> foo(other: D<out S>): S = other.prop
    fun <S> foo2(other: D<in S>): S = <!RETURN_TYPE_MISMATCH!>other.prop<!>

    fun bar(other: D<out T>): T = other.prop
    fun bar2(other: D<in T>): T = <!RETURN_TYPE_MISMATCH!>other.prop<!>

    fun baz(other: D<*>): T = <!RETURN_TYPE_MISMATCH!>other.prop<!>
}

fun <S> test1(other: D<out S>): S = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun <S> test2(other: D<in S>): S = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun test3(other: D<*>): Any? = other.prop

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, explicitBackingField, functionDeclaration, inProjection,
nullableType, outProjection, propertyDeclaration, smartcast, starProjection, typeParameter */
