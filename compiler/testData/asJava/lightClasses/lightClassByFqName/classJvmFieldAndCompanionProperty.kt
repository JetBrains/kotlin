// fields.KotlinClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package fields

class KotlinClass {
    @JvmField
    val foo: String = "1"

    companion object {
        val foo: Int = 0
    }
}
