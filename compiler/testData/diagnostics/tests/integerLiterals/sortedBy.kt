// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB

import Cause.*

typealias ChallengeFunction = suspend (String) -> Unit

enum class Cause {
    FIRST,
    SECOND,
    ERROR,
    LAST
}

class Some {
    internal val register = mutableListOf<Pair<Cause, ChallengeFunction>>()

    internal val challenges: List<ChallengeFunction>
        get() = register.filter { it.first != ERROR }.sortedBy {
            when (it.first) {
                FIRST -> 1
                SECOND -> 2
                else -> throw AssertionError()
            }
        }.map { it.second }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, functionalType, getter,
integerLiteral, lambdaLiteral, nullableType, propertyDeclaration, smartcast, suspend, typeAliasDeclaration,
whenExpression, whenWithSubject */
