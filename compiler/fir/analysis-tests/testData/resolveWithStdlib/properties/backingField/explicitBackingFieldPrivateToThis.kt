// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82721

class B<T> {
    val prop: Any?
        field = 5

    fun <S> foo(otherWithS: B<S>): Int = <!RETURN_TYPE_MISMATCH!>otherWithS.prop<!>
    fun bar(other: B<T>): Int = other.prop
}

class C<E>(val value: E) {
    val num: Any?
        field: E = value

    fun consume(v: E) {}

    fun baz(otherBaz: C<*>) {
        val it = otherBaz.num
        otherBaz.consume(<!MEMBER_PROJECTED_OUT!>it<!>)
    }

    fun <R> ban(other: C<R>) {
        val it = other.num
        other.consume(<!ARGUMENT_TYPE_MISMATCH!>it<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, integerLiteral, nullableType,
propertyDeclaration, smartcast, typeParameter */
