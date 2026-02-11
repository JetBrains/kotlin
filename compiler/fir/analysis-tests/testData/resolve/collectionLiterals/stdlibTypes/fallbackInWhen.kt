// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables

class MySomething

class MyIterable : Iterable<Int> {
    override fun iterator(): Iterator<Int> = TODO()
}

fun <X> select(vararg x: X): X = x[0]

fun test() {
    val _ = when {
        true -> [1, 2, 3]
        else -> MySomething()
    }

    val _: Any? = when {
        true -> [1, 2, 3]
        else -> MySomething()
    }

    val _ = when {
        true -> []
        else -> MyIterable()
    }

    val _ = when {
        true -> [1, 2, 3]
        else -> MyIterable()
    }

    val _ = when {
        true -> []
        true -> ["1", "2", "3"]
        else -> MyIterable()
    }

    val _: Iterable<*> = when {
        true -> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
        else -> MyIterable()
    }

    val _: Iterable<*> = when {
        true -> [1, 2, 3]
        else -> MyIterable()
    }

    val _: Iterable<*> = when {
        true -> ["1", "2", "3"]
        else -> MyIterable()
    }

    val _ = when {
        true -> [1, 2, 3]
        else -> null
    }

    val _ = when {
        true -> [1, 2, 3]
        else -> ["1", "2", "3"]
    }

    val _ = when {
        true -> []
        else -> [1, 2, 3]
    }

    val _ = when {
        true -> [1, 2, 3]
        else -> []
    }

    select([], MyIterable())
    select([1, 2, 3], MyIterable())
    select(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>, null)
    select(null, [1, 2, 3])
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, integerLiteral, localProperty, nullableType,
operator, outProjection, override, propertyDeclaration, starProjection, stringLiteral, typeParameter,
unnamedLocalVariable, vararg, whenExpression */
