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

// MODULE: enabledSubOfDisabled(disabled)
// MODULE_KIND: LibraryBinary
// JVM_DEFAULT_MODE: enable
// FILE: EnabledSubOfDisabled.kt
interface EnabledSubOfDisabled : Disabled

// MODULE: disabledSubOfEnabled(enabled)
// MODULE_KIND: LibraryBinary
// JVM_DEFAULT_MODE: disable
// FILE: DisabledSubOfEnabled.kt
interface DisabledSubOfEnabled : Enabled

// MODULE: main(enabledSubOfDisabled, disabledSubOfEnabled, disabled, enabled)
// JVM_DEFAULT_MODE: disable
// FILE: B.kt
// Whether an implementation is bridged depends on the mode of the library declaring it, not of the direct super-interface
interface B : EnabledSubOfDisabled, DisabledSubOfEnabled
