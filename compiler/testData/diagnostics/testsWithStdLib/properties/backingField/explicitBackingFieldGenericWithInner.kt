// RUN_PIPELINE_TILL: FRONTEND

class Outer<T> {
    inner class Nested<S> {
        val prop: Any?
            field = 5

        fun <S> foo(other: Outer<S>.Nested<S>): Int = other.prop
        fun <T, S> bar(other: Outer<T>.Nested<S>): Int = other.prop
        fun <T> baz(other: Outer<T>.Nested<Int>): Int = other.prop
        fun <T> baaz(other: Outer<Int>.Nested<T>): Int = other.prop
        fun baaaz(other: Outer<Int>.Nested<T>): Int = other.prop
        fun baaaaz(other: Outer<Int>.Nested<Int>): Int = other.prop
    }
}

fun <S> test1(other: Outer<S>.Nested<S>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun <T, S> test2(other: Outer<T>.Nested<S>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun <T> test3(other: Outer<T>.Nested<Int>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun <T> test4(other: Outer<Int>.Nested<T>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>
fun test5(other: Outer<Int>.Nested<Int>): Int = <!RETURN_TYPE_MISMATCH!>other.prop<!>

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, inner, integerLiteral, nullableType,
propertyDeclaration, smartcast, typeParameter */
