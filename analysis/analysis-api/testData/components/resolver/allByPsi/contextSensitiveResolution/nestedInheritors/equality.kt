// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-77823

sealed interface Settings {
    data object BasicSettings : Settings
    data object SpecificSettings : Settings
}

fun work(message: String) { }

fun usage(settings: Settings, basicSettings: Settings.BasicSettings) {
    if (settings == BasicSettings) work("Basic")

    if (basicSettings == SpecificSettings) work("Specific")
    if (basicSettings == Settings.SpecificSettings) work("Specific")
}

open class OpenSettings {
    data object OpenBasicSettings : OpenSettings()
    data object OpenSpecificSettings : OpenSettings()
}

fun usageOpen(settings: OpenSettings, basicSettings: OpenSettings.OpenBasicSettings) {
    if (settings == OpenBasicSettings) work("Basic")

    if (basicSettings == OpenSpecificSettings) work("Specific")
    if (basicSettings == OpenSettings.OpenSpecificSettings) work("Specific")
}
