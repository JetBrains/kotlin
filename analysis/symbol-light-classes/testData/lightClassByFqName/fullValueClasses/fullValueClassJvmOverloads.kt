// pack.ValueClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

value class ValueClass @JvmOverloads constructor(val first: Int, val second: String = "", val third: Long = 0L) {
    @JvmOverloads
    fun function(a: Int = 0, v: ValueClass = this, b: Long = 0L) {}
}
