// WITH_STDLIB
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Money
@file:OptIn(ExperimentalValueClassesApi::class)

package pack

@WillBecomeValue
class Money(val cents: Long) {
    override fun equals(other: Any?): Boolean = other is Money && cents == other.cents
    override fun hashCode(): Int = cents.hashCode()
    override fun toString(): String = "Money($cents)"
}
