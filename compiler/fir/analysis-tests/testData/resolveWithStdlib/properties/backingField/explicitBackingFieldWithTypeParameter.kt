// RUN_PIPELINE_TILL: FRONTEND

class A<T : Number> {
    <!INCONSISTENT_BACKING_FIELD_TYPE!>val a1: T<!>
        field: Number = 1

    val a2: Number
        field: T = <!FIELD_INITIALIZER_TYPE_MISMATCH!>1<!>

    val a3: Number
        field: T = 1 <!UNCHECKED_CAST!>as T<!>

    fun usage() {
        val a: T = a3
    }
}

class B<T> where T : Number? {
    val a: Number
        field: T&Any = null!!

    fun usage() {
        a.toDouble()
    }
}

class Box<T>(val a: T)

class C<T> {
    val a: Box<*>
        field: Box<T> = null!!

    fun usage() {
        val x: T = a.a
    }
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, dnnType, explicitBackingField,
functionDeclaration, integerLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast,
starProjection, typeConstraint, typeParameter */
