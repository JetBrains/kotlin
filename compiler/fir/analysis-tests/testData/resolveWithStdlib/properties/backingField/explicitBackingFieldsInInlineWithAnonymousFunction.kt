// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

val numbers: List<Int>
    field = mutableListOf()

inline fun anonymousFun(): () -> Unit {
    return fun() {
        numbers<!NO_SET_METHOD!>[0]<!> = 1
    }
}

private inline fun anonymousFunWithPrivate(): () -> Unit {
    return fun() {
        numbers[0] = 1
    }
}

public inline fun anonymousLambda(): () -> Unit {
    return {
        numbers<!NO_SET_METHOD!>[0]<!> = 1
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, assignment, explicitBackingField, functionDeclaration, functionalType, inline,
integerLiteral, lambdaLiteral, propertyDeclaration, smartcast */
