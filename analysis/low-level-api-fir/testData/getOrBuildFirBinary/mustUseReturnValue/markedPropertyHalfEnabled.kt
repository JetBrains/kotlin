// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=check
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
@file:MustUseReturnValues

val foo get() = "str"
