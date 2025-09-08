// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-72618
// DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER

class MatchSticksInc {
    operator fun String.inc() = this + "|"
}

class MatchSticksPlus {
    operator fun String.plus(s: String) = this + "|"
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, operator,
stringLiteral, thisExpression */
