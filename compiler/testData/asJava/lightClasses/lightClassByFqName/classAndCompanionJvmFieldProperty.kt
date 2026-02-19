// fields.KotlinClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package fields

class KotlinClass {
    val foo: String = "1"

    companion object {
        @JvmField
        val foo: Int = 0
    }
}
