// fields.KotlinClass
// LIBRARY_PLATFORMS: JVM
package fields

class KotlinClass {
    val noField: String = "1"
    val staticDelegated: String = "2"
    val instanceDelegated: String by lazy { "3" }
    val synthetic: String = "4"
    @JvmField val jvmField: String = "5"

    companion object {
        // No static field, so no clash
        val noField: Int get() = 0

        // The static field is `staticDelegated$delegate`, so no clash
        val staticDelegated: Int by lazy { 0 }

        // The instance field is `instanceDelegated$delegate`, so no clash
        val instanceDelegated: Int = 0

        // A synthetic field has no light field, but it still takes the name
        @field:JvmSynthetic val synthetic: Int = 0

        // The static field is `jvmField$delegate`, so it doesn't clash with the public instance field
        val jvmField: Int by lazy { 0 }
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: KotlinClass.class[instanceDelegated_delegate$lambda$0;jvmField_delegate$lambda$0;staticDelegated_delegate$lambda$0]
