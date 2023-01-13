// Two
// WITH_STDLIB
// STDLIB_JDK8
// FULL_JDK

import java.lang.annotation.Repeatable as JvmRepeatable

@Repeatable
@JvmRepeatable(TwoContainer::class)
annotation class Two(val name: String)
annotation class TwoContainer(val value: Array<Two>)