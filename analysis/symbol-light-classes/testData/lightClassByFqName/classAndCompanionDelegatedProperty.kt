// fields.KotlinClass
// WITH_STDLIB
package fields

class KotlinClass {
    val foo: String by lazy { "1" }

    companion object {
        val foo: Int by lazy { 0 }
    }
}
