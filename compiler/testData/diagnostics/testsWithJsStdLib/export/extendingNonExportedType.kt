// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

open class NonExportedClass

@JsExport
class ExportedClass : NonExportedClass()

interface NonExportedInterface

@JsExport
class ExportedClass2 : NonExportedInterface

@JsExport
open class ExportedGenericClass<T>

@JsExport
class ExportedClass3 : ExportedGenericClass<NonExportedClass>()

@JsExport
interface ExportedGenericInterface<T>

@JsExport
class ExportedClass4 : ExportedGenericInterface<NonExportedClass>

@JsExport
enum class ExportedEnum : ExportedGenericInterface<Any>, NonExportedInterface {
    EXPORTED_ENUM_1,
    EXPORTED_ENUM_2
}