import kotlin.test.*


fun box() {
    for (component in listOf(Int.MIN_VALUE, -1, 0, KotlinVersion.MAX_COMPONENT_VALUE, KotlinVersion.MAX_COMPONENT_VALUE + 1, Int.MAX_VALUE)) {
        for (place in 0..2) {
            val (major, minor, patch) = IntArray(3) { index -> if (index == place) component else 0 }
            if (component in 0..KotlinVersion.MAX_COMPONENT_VALUE) {
                KotlinVersion(major, minor, patch)
            } else {
                assertFailsWith<IllegalArgumentException>("Expected $major.$minor.$patch to be invalid version") {
                    KotlinVersion(major, minor, patch)
                }
            }
        }
    }
}
