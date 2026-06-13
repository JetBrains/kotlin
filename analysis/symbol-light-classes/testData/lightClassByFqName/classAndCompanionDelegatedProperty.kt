// fields.KotlinClass
// WITH_STDLIB
package fields

class KotlinClass {
    val foo: String by lazy { "1" }

    companion object {
        val foo: Int by lazy { 0 }
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: KotlinClass.class[foo_delegate$lambda$0;foo_delegate$lambda$1]
