// ISSUE: KT-77047

// MODULE: lib
// FILE: 1.kt
abstract class PmMenuWidget {
    protected inline fun widgetSpec() = "OK"
}

// MODULE: main(lib)
// FILE: 2.kt
private class GrammarSettingsWidget : PmMenuWidget() {
    fun makeSpec() = widgetSpec()
}

fun box(): String {
    return GrammarSettingsWidget().makeSpec()
}
