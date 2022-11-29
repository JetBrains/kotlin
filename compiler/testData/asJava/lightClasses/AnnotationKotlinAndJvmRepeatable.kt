// Two
// WITH_STDLIB
// STDLIB_JDK8
// FULL_JDK

@Repeatable
@JvmRepeatable(TwoContainer::class)
annotation class Two(val name: String)
annotation class TwoContainer(val value: Array<Two>)