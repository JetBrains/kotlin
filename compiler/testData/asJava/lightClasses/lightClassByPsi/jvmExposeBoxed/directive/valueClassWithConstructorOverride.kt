// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package pack

interface Interface {
    val value: Int
}

@JvmInline
value class ValueClass(override val value: Int) : Interface
