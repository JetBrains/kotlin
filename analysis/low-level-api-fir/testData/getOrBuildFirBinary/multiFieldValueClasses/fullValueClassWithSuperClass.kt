// LANGUAGE: +FullValueClasses
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: DerivedValue
package pack

sealed value class SealedValue(parameter: Int)

value class DerivedValue(val value: String) : SealedValue(0)
