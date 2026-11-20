// one.Fields
// LIBRARY_PLATFORMS: JVM
package one

class Fields(@field:JvmSynthetic val constructorProperty: Int) {
    @field:JvmSynthetic
    lateinit var lateinitProperty: String

    // A synthetic field of a regular property already gets no light field
    @field:JvmSynthetic
    val regularProperty: Int = 1
}
