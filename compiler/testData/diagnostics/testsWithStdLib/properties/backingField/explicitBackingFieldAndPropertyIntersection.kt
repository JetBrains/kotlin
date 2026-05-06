// RUN_PIPELINE_TILL: FRONTEND

interface A<T> {
    val a : Any?
    val b : T
}

open class B<K>(val v: K&Any) {
    val a: Any?
        field: Int = 5

    val b: K
        field: K&Any = v
}

class C1: A<String?>, B<String?>("OK") {
    fun usage(): String = <!RETURN_TYPE_MISMATCH!>this.b<!>
    fun usage2(other: C1): String = <!RETURN_TYPE_MISMATCH!>other.b<!>
    fun usage3(): Int = <!RETURN_TYPE_MISMATCH!>this.a<!>
}

class C2<R>(val value: R&Any) : A<R>, B<R>(value) {
    fun usage(): R&Any = <!RETURN_TYPE_MISMATCH!>this.b<!>
    fun usage2(other: C2<R>): R&Any = <!RETURN_TYPE_MISMATCH!>other.b<!>
    fun <R> usage3(other: C2<R>): R&Any = <!RETURN_TYPE_MISMATCH!>other.b<!>
    fun usage4(): Int = <!RETURN_TYPE_MISMATCH!>this.a<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, explicitBackingField, functionDeclaration, integerLiteral,
interfaceDeclaration, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, thisExpression,
typeParameter */
