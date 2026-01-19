// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND

fun <K> select(vararg k: K): K = k[0]
fun <L> id(l: L): L = l
fun cond(): Boolean = true

fun test() {
    select(
        select(
            [],
            setOf<String>(),
        ),
        setOf<Int>(),
    )

    // ambiguity
    select(
        mutableSetOf<Int>(),
        select(
            [],
            setOf<Int>(),
        ),
    )
}

fun testWhen() {
    when {
        cond() -> setOf<Int>()
        else -> when {
            cond() -> setOf<String>()
            else -> []
        }
    }

    // ambiguity
    when {
        cond() -> when {
            cond() -> setOf<Int>()
            else -> []
        }
        else -> mutableSetOf<Int>()
    }
}

fun deepTests() {
    select(
        id(id(id(
            select([], setOf<String>())
        ))),
        setOf<Int>(),
    )

    // still ambiguity
    select(
        id(id(id(
            select([], mutableSetOf<Int>()),
        ))),
        setOf<Int>(),
    )
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, intersectionType, nullableType, outProjection,
typeParameter, vararg, whenExpression */
