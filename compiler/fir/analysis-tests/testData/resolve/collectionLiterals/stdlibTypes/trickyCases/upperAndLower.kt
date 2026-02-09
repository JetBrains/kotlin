// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun <K> select(vararg k: K): K = k[0]

fun cond(): Boolean = true

fun test() {
    val a: Set<*> <!INITIALIZER_TYPE_MISMATCH!>=<!> select(mutableListOf(), [42])

    val b: Set<*> = select(setOf(), [42])
    val c: Collection<*> = select(setOf(), [42])

    // ambiguity
    val d: Collection<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)

    // ambiguity
    val e: Collection<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)

    val f: Collection<*> =
        select(
            select(
                mutableSetOf<Int>(),
                mutableSetOf<String>(),
            ),
            [],
        )

    // ambiguity
    val g: Set<*> =
        select(
            select(
                mutableSetOf<Int>(),
                mutableSetOf<String>(),
            ),
            <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>,
        )

    // ambiguity (?)
    val h: Set<*> =
        select(
            <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(),
            <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(),
            <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>,
        )

    // ambiguity (?)
    val i: Set<*> =
        select(
            select(
                <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(),
                <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(),
            ),
            <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>,
        )

    val j: Set<*> =
        select(
            select(
                setOf(),
                hashSetOf(),
            ),
            [42],
        )

    val k: Set<*> =
        select(
            hashSetOf(),
            [42],
        )
}

fun testWhen() {
    // ambiguity
    val a: Set<*> <!INITIALIZER_TYPE_MISMATCH!>=<!> when {
        cond() -> mutableListOf()
        else -> [42]
    }

    val b: Set<*> = when {
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
        else -> [42]
    }
    val c: Collection<*> = when {
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
        else -> [42]
    }

    // ambiguity
    val d: Collection<*> = when {
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>()
        else -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>
    }

    // ambiguity
    val e: Collection<*> = when {
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>()
        else -> <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>
    }

    val f: Collection<*> =
        when {
            cond() -> when {
                cond() -> mutableSetOf<Int>()
                else -> mutableSetOf<String>()
            }
            else -> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
        }

    // ambiguity
    val g: Set<*> =
        when {
            cond() -> when {
                cond() -> mutableSetOf<Int>()
                else -> mutableSetOf<String>()
            }
            else -> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
        }

    // ambiguity (?)
    val h: Set<*> =
        when {
            cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
            cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>()
            else -> [42]
        }

    // ambiguity (?)
    val i: Set<*> =
        when {
            cond() -> when {
                cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
                else -> <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>()
            }
            else -> [42]
        }

    val j: Set<*> =
        when {
            cond() -> when {
                cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
                else -> <!CANNOT_INFER_PARAMETER_TYPE!>hashSetOf<!>()
            }
            else -> [42]
        }

    val k: Set<*> =
        when {
            cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>hashSetOf<!>()
            else -> [42]
        }
}

/* GENERATED_FIR_TAGS: capturedType, collectionLiteral, functionDeclaration, integerLiteral, intersectionType,
localProperty, nullableType, outProjection, propertyDeclaration, starProjection, typeParameter, vararg, whenExpression */
