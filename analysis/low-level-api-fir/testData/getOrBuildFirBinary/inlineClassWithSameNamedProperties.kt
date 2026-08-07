// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: ValueClass
package pack

@JvmInline
value class ValueClass(val value: String) {
    val Int.value: Int get() = 0

    context(flag: Boolean)
    val value: Int get() = 1
}
