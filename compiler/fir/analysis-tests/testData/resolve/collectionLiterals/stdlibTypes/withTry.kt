// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables
// RUN_PIPELINE_TILL: FRONTEND

fun <X> id(x: X): X = x

fun cleanup(): Unit = Unit

fun test() {
    val _: Set<Int> = try {
        []
    } finally {
        cleanup()
    }

    val _: Set<Int>? = try {
        []
    } catch (e: Throwable) {
        null
    }

    val _: MutableCollection<*> = try {
        mutableSetOf(1, 2, 3)
    } catch(e: Throwable) {
        <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    }

    val _: MutableCollection<Int> = try {
        mutableSetOf()
    } catch(e: Throwable) {
        []
    }

    val _: MutableCollection<String> = try {
        <!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    } finally {
        cleanup()
    }

    val _: MutableCollection<*> = try {
        [1, 2, 3]
    } catch(e: Throwable) {
        mutableSetOf("!")
    }

    val _ = try {
        [1, 2, 3]
    } catch(e: Throwable) {
        ["1", "2", "3"]
    }

    val _ = try {
        [1, 2, 3]
    } catch(e: Throwable) {
        []
    }

    val _ = try {
        [1, 2, 3]
    } catch(e: Throwable) {
        setOf()
    }

    val _: Any = try {
        [1, 2, 3]
    } catch(e: Throwable) {
        []
    }

    val _: Collection<*> = try {
        [1, 2, 3]
    } catch(e: Throwable) {
        <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    }

    val _: Set<Int> = try {
        mutableSetOf()
    } catch(e: Throwable) {
        <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, localProperty, nullableType,
propertyDeclaration, starProjection, stringLiteral, tryExpression, typeParameter, unnamedLocalVariable */
