// one.Regular
// LANGUAGE: +CompanionBlocks
// LIBRARY_PLATFORMS: JVM
package one

class Regular {
    val staticDelegated: Int = 1
    val instanceDelegated: Int by lazy { 2 }
    val bothDelegated: Int by lazy { 3 }
    val synthetic: Int = 4
    @JvmField val jvmField: Int = 5

    companion {
        // The static field is `staticDelegated$delegate`, so no clash
        val staticDelegated: String by lazy { "staticDelegated" }

        // The instance field is `instanceDelegated$delegate`, so no clash
        val instanceDelegated: String = "instanceDelegated"

        // Both fields are `bothDelegated$delegate`, so the instance one is renamed
        val bothDelegated: String by lazy { "bothDelegated" }

        // A synthetic field has no light field, but it still takes the name
        @field:JvmSynthetic val synthetic: String = "synthetic"

        // The static field is `jvmField$delegate`, so it doesn't clash with the public instance field
        val jvmField: String by lazy { "jvmField" }
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Regular.class[bothDelegated_delegate$lambda$0;bothDelegated_delegate$lambda$1;instanceDelegated_delegate$lambda$0;jvmField_delegate$lambda$0;staticDelegated_delegate$lambda$0]
