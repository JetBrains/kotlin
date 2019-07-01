// RUNTIME_WITH_FULL_JDK
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
val s = Sam<caret> { d, t ->
    val s = "$d$t"
    listOf(s)
}