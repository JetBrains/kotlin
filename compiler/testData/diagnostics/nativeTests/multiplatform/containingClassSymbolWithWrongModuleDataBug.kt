// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1-native

@Suppress(<!ERROR_SUPPRESSION!>"CONFLICTING_OVERLOADS"<!>)
expect class AppleSSizeT {
    operator fun plus(other: AppleSSizeT): Int
    operator fun plus(other: Int): Int
}

// MODULE: m2-native()()(m1-native)

actual typealias AppleSSizeT = Int

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, infix, operator, override, stringLiteral,
typeAliasDeclaration */
