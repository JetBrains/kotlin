// LANGUAGE: +FullValueClasses
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: ValueClass
package pack

value class ValueClass<A, B>(private val first: A, val second: List<B>)
