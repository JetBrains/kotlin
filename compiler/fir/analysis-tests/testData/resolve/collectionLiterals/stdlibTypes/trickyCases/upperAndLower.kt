// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables
// RUN_PIPELINE_TILL: FRONTEND

fun <K> select(vararg k: K): K = k[0]

fun cond(): Boolean = true

fun test() {
    val _: Set<*> <!INITIALIZER_TYPE_MISMATCH!>=<!> select(mutableListOf(), [42])

    val _: Set<*> = select(setOf(), [42])
    val _: Collection<*> = select(setOf(), [42])

    // ambiguity
    val _: Collection<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)

    // ambiguity
    val _: Collection<*> = select(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)

    val _: Collection<*> =
        select(
            select(
                mutableSetOf<Int>(),
                mutableSetOf<String>(),
            ),
            [],
        )

    // ambiguity
    val _: Set<*> =
        select(
            select(
                mutableSetOf<Int>(),
                mutableSetOf<String>(),
            ),
            <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>,
        )

    // ambiguity (?)
    val _: Set<*> =
        select(
            <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(),
            <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(),
            <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>,
        )

    // ambiguity (?)
    val _: Set<*> =
        select(
            select(
                <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(),
                <!CANNOT_INFER_PARAMETER_TYPE!>mutableSetOf<!>(),
            ),
            <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>,
        )

    val _: Set<*> =
        select(
            select(
                setOf(),
                hashSetOf(),
            ),
            [42],
        )

    val _: Set<*> =
        select(
            hashSetOf(),
            [42],
        )
}

fun testWhen() {
    // ambiguity
    val _: Set<*> <!INITIALIZER_TYPE_MISMATCH!>=<!> when {
        cond() -> mutableListOf()
        else -> [42]
    }
    val _: Set<*> = when {
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
        else -> [42]
    }
    val _: Set<*> = when {
        cond() -> setOf(42)
        else -> [42]
    }
    val _: Set<Int> = when {
        cond() -> setOf()
        else -> [42]
    }
    val _: Collection<*> = when {
        cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>()
        else -> [42]
    }
    val _: Collection<Int> = when {
        cond() -> setOf()
        else -> [42]
    }

    // ambiguity
    val _: Collection<Int> = when {
        cond() -> setOf()
        cond() -> listOf()
        else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>
    }

    // ambiguity
    val _: Collection<Int> = when {
        cond() -> setOf()
        cond() -> mutableSetOf()
        else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>
    }
    val _: Collection<*> =
        when {
            cond() -> when {
                cond() -> mutableSetOf<Int>()
                else -> mutableSetOf<String>()
            }
            else -> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
        }
    val _: Collection<Any> =
        when {
            cond() -> when {
                cond() -> mutableSetOf<Int>()
                else -> mutableSetOf<String>()
            }
            else -> []
        }

    // ambiguity
    val _: Set<Any> =
        when {
            cond() -> when {
                cond() -> mutableSetOf<Int>()
                else -> mutableSetOf<String>()
            }
            else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>
        }

    // ambiguity (?)
    val _: Set<Int> =
        when {
            cond() -> setOf()
            cond() -> mutableSetOf()
            else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>
        }

    // ambiguity (?)
    val _: Set<Int> =
        when {
            cond() -> when {
                cond() -> setOf()
                else -> mutableSetOf()
            }
            else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>
        }

    val _: Set<Int> =
        when {
            cond() -> when {
                cond() -> setOf()
                else -> hashSetOf()
            }
            else -> [42]
        }

    val _: Set<*> =
        when {
            cond() -> <!CANNOT_INFER_PARAMETER_TYPE!>hashSetOf<!>()
            else -> [42]
        }

    val _: Set<Int> =
        when {
            cond() -> hashSetOf()
            else -> [42]
        }

    // ambiguity
    val _: Set<Int> =
        when {
            cond() -> when {
                cond() -> hashSetOf()
                else -> mutableSetOf()
            }
            else -> <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>
        }
    val _: MutableCollection<Any> =
        when {
            cond() -> mutableSetOf()
            else -> [42]
        }
}

/* GENERATED_FIR_TAGS: capturedType, collectionLiteral, functionDeclaration, integerLiteral, intersectionType,
localProperty, nullableType, outProjection, propertyDeclaration, starProjection, typeParameter, vararg, whenExpression */
