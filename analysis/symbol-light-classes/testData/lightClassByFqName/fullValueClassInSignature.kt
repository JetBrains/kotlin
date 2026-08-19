// pack.Usage
// LANGUAGE: +FullValueClasses

package pack

value class SingleField(val value: String)

value class MultiField(val x: Int, val y: String)

class Usage(
    val single: SingleField,
    val multi: MultiField,
) {
    var mutable: MultiField = multi

    fun consume(single: SingleField, multi: MultiField): MultiField = multi

    fun consumeAll(vararg values: MultiField): MultiField = values.first()
}
