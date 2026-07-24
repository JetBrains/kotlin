// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// RUN_PIPELINE_TILL: FRONTEND

fun testUnsupportedModernSyntax() {
    js(<!JSCODE_ERROR!>"const { a, ...rest } = { a: 1, b: 2 };"<!>)
    js(<!JSCODE_ERROR!>"class C { value = 1; }"<!>)
    js(<!JSCODE_ERROR!>"class C { static { this.value = 1; } }"<!>)
    js(<!JSCODE_ERROR!>"class C { #value = 1; }"<!>)
    js(<!JSCODE_ERROR!>"class C { #value() { return 1; } }"<!>)
    js(<!JSCODE_ERROR!>"class C { get #value() { return 1; } }"<!>)
    js(<!JSCODE_ERROR!>"class C { set #value(v) {} }"<!>)
    js(<!JSCODE_ERROR!>"2 ** 3"<!>)
    js(<!JSCODE_ERROR!>"async function f() { await 1; }"<!>)
    js(<!JSCODE_ERROR!>"a ?? b"<!>)
    js(<!JSCODE_ERROR!>"a?.b"<!>)
    js(<!JSCODE_ERROR!>"123n"<!>)
    js(<!JSCODE_ERROR!>"12_34"<!>)
}
