// FIR_IDENTICAL
// ISSUE: KT-65057
// FIR_DUMP

abstract class AbstractField<out F : AbstractField<F>>

abstract class AbstractElement<EE : AbstractElement<EE, EF>, EF : AbstractField<EF>>

interface ElementOrRef<RE : AbstractElement<RE, RF>, RF : AbstractField<RF>> {
    val element: RE
}

fun foo(x: ElementOrRef<*, *>) = x.element

interface FieldOrRef<FF : AbstractField<FF>> {
    val field: FF
}

fun bar(y: FieldOrRef<*>) = y.field
