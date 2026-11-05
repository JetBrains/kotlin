// WITH_STDLIB
@file:OptIn(ExperimentalValueClassesApi::class)

@WillBecomeValue
class Mo<caret>ney(val cents: Long) {
    override fun equals(other: Any?): Boolean = other is Money && cents == other.cents
    override fun hashCode(): Int = cents.hashCode()
    override fun toString(): String = "Money($cents)"
}
