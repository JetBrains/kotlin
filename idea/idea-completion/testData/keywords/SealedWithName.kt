// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+MultiPlatformProjects

seal<caret>
// EXIST: "sealed class SealedWithName"
// EXIST: "sealed interface SealedWithName"
// EXIST: "sealed class"
// EXIST: "sealed interface"
// NOTHING_ELSE