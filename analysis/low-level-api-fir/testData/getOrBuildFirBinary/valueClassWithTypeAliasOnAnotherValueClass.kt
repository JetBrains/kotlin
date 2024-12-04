// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: ValueClass
package pack

@JvmInline
value class AnotherValueClass(val s: String)

typealias MyTypeAlias = AnotherValueClass

@JvmInline
value class ValueClass(val value: MyTypeAlias)