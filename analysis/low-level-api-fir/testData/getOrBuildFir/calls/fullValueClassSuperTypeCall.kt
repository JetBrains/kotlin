// LANGUAGE: +FullValueClasses
// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

sealed value class Result

value class Success(val value: String, val code: Int) : <expr>Result()</expr>
