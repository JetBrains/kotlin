// WITH_STDLIB
@file:OptIn(ExperimentalValueClassesApi::class)

@WillBecomeValue
object No<caret>Money {
    override fun toString(): String = "NoMoney"
}
