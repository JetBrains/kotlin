// LANGUAGE: +FullValueClasses
// LIBRARY_PLATFORMS: JVM

package full

value class SingleField(val value: String) {
    val size: Int get() = value.length

    fun duplicate(): SingleField = this
}

value class MultiField(val x: Int, val y: String) {
    constructor(x: Long, y: CharSequence) : this(x.toInt(), y.toString())

    val description: String get() = "$x: $y"

    fun replace(other: MultiField): MultiField = other

    companion object {
        @JvmStatic
        fun create(x: Int, y: String): MultiField = MultiField(x, y)
    }
}

value object Marker {
    val label: String get() = "marker"

    fun member(): String = label
}

class Usage(
    val single: SingleField,
    val multi: MultiField,
) {
    var mutable: MultiField = multi

    fun consume(single: SingleField, multi: MultiField): MultiField = multi

    fun consumeAll(vararg values: MultiField): MultiField = values.first()
}

fun topLevel(single: SingleField, multi: MultiField): MultiField = multi
