// "Create local variable 'defaultPreferencesName'" "false"
// ACTION: Create parameter 'defaultPreferencesName'
// ACTION: Create property 'defaultPreferencesName'
// ACTION: Rename reference
// ERROR: Unresolved reference: defaultPreferencesName
// ERROR: Default value of annotation parameter must be a compile-time constant

class AppModule {
    val defaultPreferencesName  = "defaultPreferences"

    annotation class Preferences(val type: String = <caret>defaultPreferencesName)
}