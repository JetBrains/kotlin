// Two
// FULL_JDK

@java.lang.annotation.Repeatable(TwoContainer::class)
annotation class Two(val name: String)
annotation class TwoContainer(val value: Array<Two>)