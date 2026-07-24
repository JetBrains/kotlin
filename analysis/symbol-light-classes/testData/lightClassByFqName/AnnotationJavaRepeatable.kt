// Two
// STDLIB_JDK8
// FULL_JDK
// LIBRARY_PLATFORMS: JVM

@java.lang.annotation.Repeatable(TwoContainer::class)
annotation class Two(val name: String)
annotation class TwoContainer(val value: Array<Two>)
