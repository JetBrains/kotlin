// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class PublicValue(val `underlying property`: String)

@JvmInline
value class PrivateValue(private val `underlying property`: String)
