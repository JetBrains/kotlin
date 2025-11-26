// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82721

class B<T> {
    val prop: Any?
        field = 5

    fun <S> foo(otherWithS: B<S>): Int = otherWithS.prop
    fun bar(other: B<T>): Int = other.prop
}

class C<E>(val value: E) {
    val num: Any?
        field: E = value

    fun consume(v: E) {}

    fun baz(otherBaz: C<*>) {
        val it = otherBaz.num
        otherBaz.consume(it)
    }

    fun <R> ban(other: C<R>) {
        val it = other.num
        other.consume(it)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, integerLiteral, nullableType,
propertyDeclaration, smartcast, typeParameter */
