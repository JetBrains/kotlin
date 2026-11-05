// WITH_STDLIB
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtObjectDeclaration
// MAIN_FILE_NAME: NoMoney
@file:OptIn(ExperimentalValueClassesApi::class)

package pack

@WillBecomeValue
object NoMoney {
    override fun toString(): String = "NoMoney"
}
