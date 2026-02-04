// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-77823

sealed interface Settings {
    data object BasicSettings : Settings
    data object SpecificSettings : Settings
}

fun work(message: String) { }

fun usage(settings: Settings, basicSettings: Settings.BasicSettings) {
    if (settings == BasicSettings) work("Basic")

    if (<!PROBLEMATIC_EQUALS!>basicSettings == SpecificSettings<!>) work("Specific")
    if (<!PROBLEMATIC_EQUALS!>basicSettings == Settings.SpecificSettings<!>) work("Specific")
}

open class OpenSettings {
    data object OpenBasicSettings : OpenSettings()
    data object OpenSpecificSettings : OpenSettings()
}

fun usageOpen(settings: OpenSettings, basicSettings: OpenSettings.OpenBasicSettings) {
    if (settings == OpenBasicSettings) work("Basic")

    if (basicSettings == <!UNRESOLVED_REFERENCE!>OpenSpecificSettings<!>) work("Specific")
    if (<!PROBLEMATIC_EQUALS!>basicSettings == OpenSettings.OpenSpecificSettings<!>) work("Specific")
}

/* GENERATED_FIR_TAGS: data, equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, nestedClass,
objectDeclaration, sealed, stringLiteral */
