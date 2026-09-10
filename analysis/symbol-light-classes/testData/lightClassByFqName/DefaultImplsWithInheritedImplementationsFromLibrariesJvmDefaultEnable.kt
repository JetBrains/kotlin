// B

// MODULE: disabled
// MODULE_KIND: LibraryBinary
// JVM_DEFAULT_MODE: disable
// FILE: Disabled.kt
interface Disabled {
    fun fromDisabled() {}
}

// MODULE: enabled
// MODULE_KIND: LibraryBinary
// JVM_DEFAULT_MODE: enable
// FILE: Enabled.kt
interface Enabled {
    fun fromEnabled() {}
}

// MODULE: noCompatibility
// MODULE_KIND: LibraryBinary
// JVM_DEFAULT_MODE: no-compatibility
// FILE: NoCompatibility.kt
interface NoCompatibility {
    fun fromNoCompatibility() {}
}

// MODULE: main(disabled, enabled, noCompatibility)
// The JS library compilation doesn't see the dependency modules
// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: enable
// FILE: B.kt
import kotlin.coroutines.CoroutineContext

// The JVM standard library is compiled with `-jvm-default=disable`
interface B : Disabled, Enabled, NoCompatibility, ClosedRange<Int>, CoroutineContext.Element {
    fun declared() {}
}
