// pack.MultiField
// LANGUAGE: +FullValueClasses
// LIBRARY_PLATFORMS: JVM

package pack

value class MultiField(val x: Int, val y: String) {
    constructor(x: Long, y: CharSequence) : this(x.toInt(), y.toString())

    val description: String get() = "$x: $y"

    fun replace(other: MultiField): MultiField = other

    companion object {
        @JvmStatic
        fun create(x: Int, y: String): MultiField = MultiField(x, y)
    }
}
