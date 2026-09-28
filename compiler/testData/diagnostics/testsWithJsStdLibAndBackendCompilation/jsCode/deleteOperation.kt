// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// RUN_PIPELINE_TILL: CODEGEN

// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Any) {
    js("delete x.foo;")
    js("delete x['bar'];")
    js(<!JSCODE_ERROR!>"delete x.baz();"<!>)
    js(<!JSCODE_ERROR!>"delete this;"<!>)
}
