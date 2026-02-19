// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: ValueClass
package pack

typealias MyTypeAlias<T> = List<T>

@JvmInline
value class ValueClass<T>(val value: MyTypeAlias<T>)