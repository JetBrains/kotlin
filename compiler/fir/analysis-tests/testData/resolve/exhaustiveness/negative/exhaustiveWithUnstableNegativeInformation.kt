// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +DataFlowBasedExhaustiveness

fun upperType(): Int {
    var a: Boolean? = false
    val block: () -> Unit = { a = null }

    if (a == null) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (a) {
        true -> 2
        false -> 3
    }
}

fun booleanLiteral(): Int {
    var a: Boolean = true
    val block: () -> Unit = { a = false }

    if (a == false) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (a) {
        true -> 2
    }
}

enum class EnumBoolean { False, True }

fun enumEntry(): Int {
    var a: EnumBoolean = EnumBoolean.True
    val block: () -> Unit = { a = EnumBoolean.False }

    if (a == EnumBoolean.False) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (a) {
        EnumBoolean.True -> 2
    }
}

sealed class SealedBoolean {
    data object True : SealedBoolean()
    data object False : SealedBoolean()
}

fun sealedVariant(): Int {
    var a: SealedBoolean = SealedBoolean.True
    val block: () -> Unit = { a = SealedBoolean.False }

    if (a == SealedBoolean.False) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (a) {
        is SealedBoolean.True -> 2
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, data, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, functionalType, ifExpression, integerLiteral, isExpression, lambdaLiteral, localProperty,
nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
