// FIR_IDENTICAL
// ISSUE: KT-57814
import kotlin.reflect.KProperty

fun <P2> xComponent(
    builder: (prop1: P2) -> Unit
): (prop1: P2) -> Unit = {}

operator fun <P1> ((prop1: P1) -> Unit).getValue(
    thisRef: Any?,
    property: KProperty<*>
): (prop1: P1) -> Unit = this

val pdfDocumentViewer by xComponent { _: String? ->
}

fun xPDFDocumentViewer(
    href: String?
) = pdfDocumentViewer(
    href // Should be OK to pass nullable String? there
)
