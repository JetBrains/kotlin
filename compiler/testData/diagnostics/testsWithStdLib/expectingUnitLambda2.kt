// FIR_IDENTICAL
// WITH_REFLECT
// ISSUE: KT-58136

import kotlin.reflect.KProperty

fun <P1> xComponent(
    builder: (prop1: P1) -> Unit
): (prop1: P1) -> Unit = {}

operator fun <P1> ((prop1: P1) -> Unit).getValue(
    thisRef: Any?,
    property: KProperty<*>
): (prop1: P1) -> Unit = this

val pdfDocumentViewer by xComponent { clearFilter: () -> Unit ->
}

fun foo() {
    val resetFilters: () -> Any = { // Change return type to Any or String here
        "hello"
    }

    pdfDocumentViewer(resetFilters) // Argument type mismatch: actual type is kotlin/Function0<kotlin/Any> but @R|kotlin/ParameterName|(name = String(prop1))  kotlin/Function0<kotlin/Unit> was expected
}
