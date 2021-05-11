// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+MultiPlatformProjects

class OuterClass {
    seal<caret>
}
// EXIST: "sealed class"
// EXIST: "sealed interface"
// NOTHING_ELSE