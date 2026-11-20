// WITH_STDLIB

package pack

@OptIn(ExperimentalValueClassesApi::class)
@WillBecomeValue
class Money(val amount: Long, val currency: String) {
    override fun equals(other: Any?): Boolean = other is Money && amount == other.amount && currency == other.currency

    override fun hashCode(): Int = 31 * amount.hashCode() + currency.hashCode()

    override fun toString(): String = "$amount $currency"
}

@OptIn(ExperimentalValueClassesApi::class)
@WillBecomeValue
object NoMoney {
    override fun toString(): String = "NoMoney"
}
