// FIR_IDENTICAL
// ISSUE: KT-63487
// FIR_DUMP

abstract class AbstractField<out Field : AbstractField<Field>>

abstract class AbstractElement<Element : AbstractElement<Element, Field>, Field : AbstractField<Field>>

interface ElementOrRef<Element : AbstractElement<Element, Field>, Field : AbstractField<Field>> {
    val element: Element
}

fun elvis(x: ElementOrRef<*, *>?, y: ElementOrRef<*, *>?) {
    val xElement = x?.element
    val yElement = y?.element
    val e = xElement ?: yElement
}
