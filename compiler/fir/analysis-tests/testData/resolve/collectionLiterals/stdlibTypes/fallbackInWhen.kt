// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables

class MySomething

class MyIterable : Iterable<Int> {
    override fun iterator(): Iterator<Int> = TODO()
}

fun <X> select(vararg x: X): X = x[0]

fun test() {
    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> MySomething()
    }

    val _: Any? = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> MySomething()
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
        else -> MyIterable()
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> MyIterable()
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>
        else -> MyIterable()
    }

    val _: Iterable<*> = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
        else -> MyIterable()
    }

    val _: Iterable<*> = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> MyIterable()
    }

    val _: Iterable<*> = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>
        else -> MyIterable()
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> null
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
        else -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    }

    val _ = when {
        true -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        else -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    }

    select(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, MyIterable())
    select(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>, MyIterable())
    select(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, null)
    select(null, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, integerLiteral, localProperty, nullableType,
operator, outProjection, override, propertyDeclaration, starProjection, stringLiteral, typeParameter,
unnamedLocalVariable, vararg, whenExpression */
