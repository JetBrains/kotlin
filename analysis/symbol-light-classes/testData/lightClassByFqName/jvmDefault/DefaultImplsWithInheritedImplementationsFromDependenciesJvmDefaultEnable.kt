// B

// MODULE: disabled
// JVM_DEFAULT_MODE: disable
// FILE: Disabled.kt
interface Disabled {
    fun fromDisabled() {}
}

// MODULE: enabled
// JVM_DEFAULT_MODE: enable
// FILE: Enabled.kt
interface Enabled {
    fun fromEnabled() {}
}

// MODULE: main(disabled, enabled)
// JVM_DEFAULT_MODE: enable
// FILE: B.kt
interface SameModule {
    fun fromSameModule() {}
}

interface B : SameModule, Disabled, Enabled {
    fun declared() {}
}
