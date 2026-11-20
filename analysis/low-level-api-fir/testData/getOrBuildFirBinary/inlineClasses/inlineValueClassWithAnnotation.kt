// LANGUAGE: +FullValueClasses
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: AnnotatedValue
package pack

annotation class Anno

@Anno
inline value class AnnotatedValue(val value: String)
