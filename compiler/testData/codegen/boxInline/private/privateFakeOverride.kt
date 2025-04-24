// ISSUE: KT-77047
// SKIP_UNBOUND_IR_SERIALIZATION
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// ^^^ KT-77047: Unexpected inline function without body: FUN IR_EXTERNAL_DECLARATION_STUB name:widgetSpec visibility:protected modality:FINAL <> (<this>:boxInline.private.privateFakeOverride.PmMenuWidget) returnType:kotlin.String [inline]

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
