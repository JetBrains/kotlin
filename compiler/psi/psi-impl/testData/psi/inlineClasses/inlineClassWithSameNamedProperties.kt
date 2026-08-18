// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class PublicValue(val value: String) {
    val Int.value: Int get() = 0

    context(flag: Boolean)
    val value: Int get() = 1
}
