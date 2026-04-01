// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS

// FILE: main.kt
package foo

interface NotExported
interface NotExported2
interface NotExported3

@JsExport
interface Exported1 : <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported")!>NotExported<!>

@JsExport
interface ExportedSuper

// No diagnostics: both are exported
@JsExport
interface Good : ExportedSuper

// Only the not-exported supertype should be reported
@JsExport
interface Mixed : <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported")!>NotExported<!>, ExportedSuper

// Multiple not-exported supertypes should all be reported
@JsExport
interface ManyNotExportedSupers :
    <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported")!>NotExported<!>,
    <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported2")!>NotExported2<!>,
    <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported3")!>NotExported3<!>

// No diagnostics: the sub-interface itself is not exported
interface NotExportedSub : NotExported

// FILE: fileLevel.kt
@file:JsExport

package foo

// Exported by file annotation; supertype is not exported → diagnostic
interface ExportedByFile : <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported")!>NotExported<!>
