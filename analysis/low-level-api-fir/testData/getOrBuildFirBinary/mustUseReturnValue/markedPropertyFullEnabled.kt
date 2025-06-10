// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
@file:MustUseReturnValue

val foo get() = "str"
