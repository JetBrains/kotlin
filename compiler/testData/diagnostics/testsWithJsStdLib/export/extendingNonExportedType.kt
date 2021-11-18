// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

open class NonExportedClass

@JsExport
class <!NON_EXPORTABLE_TYPE("super; NonExportedClass")!>ExportedClass<!> : NonExportedClass()

interface NonExportedInterface

@JsExport
class <!NON_EXPORTABLE_TYPE("super; NonExportedInterface")!>ExportedClass2<!> : NonExportedInterface

@JsExport
open class ExportedGenericClass<T>

@JsExport
class <!NON_EXPORTABLE_TYPE("super; ExportedGenericClass<NonExportedClass>")!>ExportedClass3<!> : ExportedGenericClass<NonExportedClass>()

@JsExport
interface ExportedGenericInterface<T>

@JsExport
class <!NON_EXPORTABLE_TYPE("super; ExportedGenericInterface<NonExportedClass>")!>ExportedClass4<!> : ExportedGenericInterface<NonExportedClass>
