// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +JsAllowExportTypealiases
package foo

@JsExport
typealias ExportableTypealias = Int

@JsExport.Ignore
open class Foo

@JsExport
typealias AliasToNotExportableType = <!NON_EXPORTABLE_TYPE!>Foo<!>

@JsExport
typealias UsageOfNotExportableTypeInTypeArgs = <!NON_EXPORTABLE_TYPE!>Pair<String, Foo><!>
